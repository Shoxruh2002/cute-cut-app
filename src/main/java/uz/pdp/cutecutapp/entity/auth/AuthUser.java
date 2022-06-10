package uz.pdp.cutecutapp.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.pdp.cutecutapp.entity.Auditable;
import uz.pdp.cutecutapp.enums.Language;
import uz.pdp.cutecutapp.enums.Role;
import uz.pdp.cutecutapp.enums.Status;

import javax.persistence.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "phoneRole", columnList = "phoneNumber, role", unique = true)
})
public class AuthUser extends Auditable {

    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    private String password;

    private boolean isBusy;

    private Language language;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private Long pictureId;

    //    @Column(nullable = false)
    private Long organizationId;

    private Long barberShopId;

    public AuthUser(String phoneNumber, String password, Role role, Boolean isBusy) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
        this.isBusy = isBusy;
    }
}
