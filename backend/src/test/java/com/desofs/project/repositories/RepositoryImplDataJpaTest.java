package com.desofs.project.repositories;

import com.desofs.project.audit.mapper.AuditPersistenceMapper;
import com.desofs.project.audit.model.AuditLog;
import com.desofs.project.audit.repositories.AuditRepository;
import com.desofs.project.audit.repositories.AuditRepositoryImpl;
import com.desofs.project.department.mapper.DepartmentPersistenceMapper;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.department.repositories.DepartmentRepositoryImpl;
import com.desofs.project.document.mapper.DocumentPersistenceMapper;
import com.desofs.project.document.model.Document;
import com.desofs.project.document.repositories.DocumentRepository;
import com.desofs.project.document.repositories.DocumentRepositoryImpl;
import com.desofs.project.user.mapper.UserPersistenceMapper;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import com.desofs.project.user.repositories.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        UserRepositoryImpl.class,
        DepartmentRepositoryImpl.class,
        DocumentRepositoryImpl.class,
        AuditRepositoryImpl.class,
        UserPersistenceMapper.class,
        DepartmentPersistenceMapper.class,
        DocumentPersistenceMapper.class,
        AuditPersistenceMapper.class
})
class RepositoryImplDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    void userRepository_CoversQueriesPagingSaveAndDeleteAll() {
        User admin = userRepository.save(user("admin", "admin@example.com", Role.ADMIN));
        User normalUser = userRepository.save(user("user", "user@example.com", Role.USER));

        assertThat(userRepository.findByUsername("admin")).contains(admin);
        assertThat(userRepository.findByUsername("missing")).isEmpty();
        assertThat(userRepository.findByEmail("user@example.com")).contains(normalUser);
        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
        assertThat(userRepository.findAll()).extracting(User::getUsername).contains("admin", "user");
        assertThat(userRepository.findById(admin.getId())).contains(admin);
        assertThat(userRepository.findAllById(List.of(admin.getId(), normalUser.getId())))
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("admin", "user");
        assertThat(userRepository.findAllById(List.<UUID>of())).isEmpty();

        Page<User> allUsers = userRepository.listUsers(null, PageRequest.of(0, 10));
        Page<User> admins = userRepository.listUsers(Role.ADMIN, PageRequest.of(0, 10));

        assertThat(allUsers.getTotalElements()).isEqualTo(2);
        assertThat(admins.getContent()).extracting(User::getUsername).containsExactly("admin");

        admin.setEmail("admin2@example.com");
        assertThat(userRepository.save(admin).getEmail()).isEqualTo("admin2@example.com");

        userRepository.deleteAll();
        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void departmentRepository_CoversQueriesPagingSaveAndDelete() {
        User manager = userRepository.save(user("manager", "manager@example.com", Role.MANAGER));
        Department engineering = departmentRepository.save(Department.builder()
                .name("Engineering")
                .description("Software")
                .createdBy(manager.getId())
                .updatedBy(manager.getId())
                .managers(Set.of(manager))
                .build());
        Department operations = departmentRepository.save(Department.builder()
                .name("Operations")
                .description("Ops")
                .createdBy(manager.getId())
                .build());

        assertThat(departmentRepository.findByName("Engineering"))
                .map(Department::getId)
                .contains(engineering.getId());
        assertThat(departmentRepository.findByName("Missing")).isEmpty();
        assertThat(departmentRepository.existsByName("Engineering")).isTrue();
        assertThat(departmentRepository.existsByName("Missing")).isFalse();
        assertThat(departmentRepository.findAll()).extracting(Department::getName)
                .contains("Engineering", "Operations");
        assertThat(departmentRepository.findById(engineering.getId()))
                .map(Department::getName)
                .contains("Engineering");

        assertThat(departmentRepository.listDepartments(null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
        assertThat(departmentRepository.listDepartments("engi", PageRequest.of(0, 10)).getContent())
                .extracting(Department::getName)
                .containsExactly("Engineering");

        engineering.setDescription("Platform");
        assertThat(departmentRepository.save(engineering).getDescription()).isEqualTo("Platform");

        departmentRepository.delete(operations);
        assertThat(departmentRepository.findById(operations.getId())).isEmpty();
        departmentRepository.deleteAll();
        assertThat(departmentRepository.findAll()).isEmpty();
    }

    @Test
    void documentRepository_CoversSaveAccessQueriesPagingAndDelete() {
        User owner = userRepository.save(user("owner", "owner@example.com", Role.USER));
        User admin = userRepository.save(user("docadmin", "docadmin@example.com", Role.ADMIN));
        User manager = userRepository.save(user("docmanager", "docmanager@example.com", Role.MANAGER));
        Department department = departmentRepository.save(Department.builder()
                .name("Documents")
                .description("Docs")
                .createdBy(admin.getId())
                .managers(Set.of(manager))
                .build());

        Document departmental = documentRepository.save(Document.builder()
                .filename("plan.txt")
                .filepath("/tmp/desofs/plan.txt")
                .contentType("text/plain")
                .size(4L)
                .owner(owner)
                .department(department)
                .build());
        Document personal = documentRepository.save(Document.builder()
                .filename("notes.txt")
                .filepath("/tmp/desofs/notes.txt")
                .contentType("text/plain")
                .size(5L)
                .owner(owner)
                .build());

        assertThat(documentRepository.findAll()).extracting(Document::getFilename)
                .contains("plan.txt", "notes.txt");
        assertThat(documentRepository.findById(departmental.getId()))
                .map(Document::getFilename)
                .contains("plan.txt");
        assertThat(documentRepository.findByOwnerId(owner.getId())).hasSize(2);

        Page<Document> adminPage = documentRepository.listDocuments(
                admin.getId(), Role.ADMIN, "plan", department.getId(), PageRequest.of(0, 10));
        Page<Document> ownerPage = documentRepository.listDocuments(
                owner.getId(), Role.USER, null, null, PageRequest.of(0, 10));
        Page<Document> managerPage = documentRepository.listDocuments(
                manager.getId(), Role.MANAGER, "PLAN", department.getId(), PageRequest.of(0, 10));

        assertThat(adminPage.getContent()).extracting(Document::getFilename).containsExactly("plan.txt");
        assertThat(ownerPage.getTotalElements()).isEqualTo(2);
        assertThat(managerPage.getContent()).extracting(Document::getFilename).containsExactly("plan.txt");

        departmental.setFilename("plan-v2.txt");
        assertThat(documentRepository.save(departmental).getFilename()).isEqualTo("plan-v2.txt");

        documentRepository.delete(personal);
        assertThat(documentRepository.findById(personal.getId())).isEmpty();
        documentRepository.deleteAll();
        assertThat(documentRepository.findAll()).isEmpty();
    }

    @Test
    void listQueries_TreatLikeWildcardsAsLiteralSearchText() {
        User owner = userRepository.save(user("literal-owner", "literal-owner@example.com", Role.USER));
        User manager = userRepository.save(user("literal-manager", "literal-manager@example.com", Role.MANAGER));
        departmentRepository.save(Department.builder()
                .name("100%_Security")
                .description("Literal wildcard department")
                .createdBy(manager.getId())
                .build());
        departmentRepository.save(Department.builder()
                .name("100xxSecurity")
                .description("Wildcard-looking alternative")
                .createdBy(manager.getId())
                .build());

        documentRepository.save(Document.builder()
                .filename("100%_plan.txt")
                .filepath("/tmp/desofs/100-percent-plan.txt")
                .contentType("text/plain")
                .size(4L)
                .owner(owner)
                .build());
        documentRepository.save(Document.builder()
                .filename("100xxplan.txt")
                .filepath("/tmp/desofs/100xxplan.txt")
                .contentType("text/plain")
                .size(5L)
                .owner(owner)
                .build());

        assertThat(departmentRepository.listDepartments("100%_", PageRequest.of(0, 10)).getContent())
                .extracting(Department::getName)
                .containsExactly("100%_Security");
        assertThat(documentRepository.listDocuments(
                owner.getId(), Role.USER, "100%_", null, PageRequest.of(0, 10)).getContent())
                .extracting(Document::getFilename)
                .containsExactly("100%_plan.txt");
    }

    @Test
    void auditRepository_CoversAppendOnlySaveAndQueries() {
        UUID actorId = UUID.randomUUID();
        AuditLog created = auditRepository.save(AuditLog.builder()
                .action("CREATE")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .userId(actorId)
                .details("created")
                .build());
        auditRepository.save(AuditLog.builder()
                .action("DELETE")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID())
                .details("deleted")
                .build());

        assertThat(created.getId()).isNotNull();
        assertThat(auditRepository.findAll()).hasSize(2);
        assertThat(auditRepository.findByUserId(actorId)).extracting(AuditLog::getAction)
                .containsExactly("CREATE");
        assertThatThrownBy(() -> auditRepository.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .action("UPDATE")
                .entityType("Document")
                .entityId(UUID.randomUUID().toString())
                .userId(actorId)
                .build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static User user(String username, String email, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .password("encoded")
                .role(role)
                .build();
    }
}
