package uz.pdp.cutecutapp.session;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.pdp.cutecutapp.entity.auth.AuthUser;
import uz.pdp.cutecutapp.repository.auth.AuthUserRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionUser {

    private final AuthUserRepository repository;


    public String getUsername() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Long getId() {
        Optional<AuthUser> user = repository.findByPhoneNumberAndDeletedFalse(this.getUsername());
        return user.get().getId();
    }

    public Long getOrgId() {
        Optional<AuthUser> user = repository.findByPhoneNumberAndDeletedFalse(this.getUsername());
        return user.get().getOrganizationId();
    }
}
