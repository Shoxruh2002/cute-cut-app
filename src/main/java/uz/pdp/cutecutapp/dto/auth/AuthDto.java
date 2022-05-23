package uz.pdp.cutecutapp.dto.auth;

import lombok.Getter;
import lombok.Setter;
import uz.pdp.cutecutapp.dto.GenericDto;
import uz.pdp.cutecutapp.enums.Role;


public class AuthDto extends GenericDto {

    public String fullName;

    public String username;

    public String phoneNumber;

    public Long organizationId;

    public String picturePath;

    public Role role;

}
