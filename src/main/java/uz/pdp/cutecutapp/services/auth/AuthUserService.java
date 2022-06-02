package uz.pdp.cutecutapp.services.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.pdp.cutecutapp.criteria.BaseCriteria;
import uz.pdp.cutecutapp.dto.auth.*;
import uz.pdp.cutecutapp.dto.otp.OtpResponse;
import uz.pdp.cutecutapp.dto.responce.AppErrorDto;
import uz.pdp.cutecutapp.dto.responce.DataDto;
import uz.pdp.cutecutapp.entity.auth.AuthUser;
import uz.pdp.cutecutapp.entity.auth.Device;
import uz.pdp.cutecutapp.entity.auth.PhoneCode;
import uz.pdp.cutecutapp.enums.Role;
import uz.pdp.cutecutapp.enums.Status;
import uz.pdp.cutecutapp.mapper.auth.AuthUserMapper;
import uz.pdp.cutecutapp.properties.OtpProperties;
import uz.pdp.cutecutapp.properties.ServerProperties;
import uz.pdp.cutecutapp.repository.auth.AuthUserRepository;
import uz.pdp.cutecutapp.repository.auth.DeviceRepository;
import uz.pdp.cutecutapp.repository.auth.PhoneCodeRepository;
import uz.pdp.cutecutapp.services.AbstractService;
import uz.pdp.cutecutapp.services.GenericCrudService;
import uz.pdp.cutecutapp.session.SessionUser;
import uz.pdp.cutecutapp.utils.JwtUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
public class AuthUserService extends AbstractService<AuthUserRepository, AuthUserMapper> implements UserDetailsService, GenericCrudService<AuthUser, AuthDto, AuthCreateDto, AuthUpdateDto, BaseCriteria, Long> {


    private final PhoneCodeRepository phoneCodeRepository;
    private final OtpProperties otpProperties;
    private final ObjectMapper objectMapper;
    private final ServerProperties serverProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;

    private final SessionUser sessionUser;
    private final DeviceRepository deviceRepository;
    private Path root = Paths.get("C:\\uploads");

    public AuthUserService(AuthUserRepository repository, AuthUserMapper mapper, PhoneCodeRepository phoneCodeRepository, OtpProperties otpProperties, ObjectMapper objectMapper, ServerProperties serverProperties, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, OtpService otpService, SessionUser sessionUser, DeviceRepository deviceRepository) {
        super(repository, mapper);
        this.phoneCodeRepository = phoneCodeRepository;
        this.otpProperties = otpProperties;
        this.objectMapper = objectMapper;
        this.serverProperties = serverProperties;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
        this.sessionUser = sessionUser;
        this.deviceRepository = deviceRepository;
    }

    //    @PostConstruct
    public void init() {
        try {
            Files.createDirectory(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AuthUser> optional = repository.findByPhoneNumberAndDeletedFalse(username);
        if (optional.isPresent()) {
            AuthUser user = optional.get();
            if (user.getStatus().equals(Status.ACTIVE)) {
                return User.builder()
                        .username(user.getPhoneNumber())
                        .password(user.getPassword())
                        .authorities(new SimpleGrantedAuthority(user.getRole().name()))
                        .build();
            } else throw new RuntimeException("User Not Active");
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public AuthUser loadAuthUserByUsername(String username) {
        Optional<AuthUser> optional = repository.findByPhoneNumberAndDeletedFalse(username);
        return optional.orElse(null);
    }


    public DataDto<SessionDto> login(AuthUserPasswordDto dto) {
        try {
            HttpClient httpclient = HttpClientBuilder.create().build();
            String url = serverProperties.getUrl()+"/api/login";
            HttpPost httppost = new HttpPost(url);
            AuthLoginDto loginDto = new AuthLoginDto(dto.phoneNumber, dto.password);
            byte[] bytes = objectMapper.writeValueAsBytes(loginDto);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httppost.setEntity(new InputStreamEntity(byteArrayInputStream));

            HttpResponse response = httpclient.execute(httppost);

            JsonNode json_auth = objectMapper.readTree(EntityUtils.toString(response.getEntity()));

            if (json_auth.has("data")) {
                Optional<AuthUser> authUser = repository.findByPhoneNumberAndDeletedFalse(dto.phoneNumber);
                deviceRepository.save(new Device(authUser.get().getId(), dto.deviceId, dto.deviceToken));
                JsonNode node = json_auth.get("data");
                SessionDto sessionDto = objectMapper.readValue(node.toString(), SessionDto.class);
                return new DataDto<>(sessionDto);
            }
            return new DataDto<>(new AppErrorDto("bad Request", "", HttpStatus.BAD_REQUEST));

        } catch (IOException e) {
            return new DataDto<>(new AppErrorDto("bad request", "", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    public DataDto<Boolean> register(AuthUserPhoneDto dto) {
        try {
            String phoneNumber = String.format("+998%s", dto.phoneNumber);
            Optional<AuthUser> authUser = repository.findByPhoneNumberAndDeletedFalse(phoneNumber);
            if (authUser.isPresent())
                return new DataDto<>(new AppErrorDto(HttpStatus.ALREADY_REPORTED, "phone number available", "/auth/register"));
            OtpResponse send = otpService.send(new AuthUserPhoneDto(phoneNumber));
            if (send.success) {
                LocalDateTime expire = LocalDateTime.now().plusMinutes(otpProperties.getExpiration());
                PhoneCode phoneCode = new PhoneCode(phoneNumber, send.code.toString(), expire);
                phoneCodeRepository.save(phoneCode);
                return new DataDto<>(Boolean.TRUE, HttpStatus.OK.value());
            }
            return new DataDto<>(Boolean.FALSE, HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {
            return new DataDto<>(new AppErrorDto(HttpStatus.BAD_REQUEST, "Bad request ", "/auth/register"));
        }
    }

    @Override
    public DataDto<Long> create(AuthCreateDto dto) {
        try {
//            if (!sessionUser.getRole().equals(Role.ADMIN)) {
//                return new DataDto<>(new AppErrorDto(HttpStatus.BAD_REQUEST, "role does not exist", "/auth/create"));
//            }
            Role role = Role.ADMIN.checkRole(dto.getRole());
            AuthUser authUser = mapper.fromCreateDto(dto);
            authUser.setRole(role);
            if (Objects.nonNull(authUser.getPassword())) {
                authUser.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            return new DataDto<>(repository.save(authUser).getId());
        } catch (Exception e) {
            return new DataDto<>(new AppErrorDto(HttpStatus.IM_USED, "already Taken phone", "auth/user/create"));
        }
    }

    @Override
    public DataDto<AuthDto> get(Long id) {
        if (Objects.isNull(id)) return new DataDto<>(new AppErrorDto("Bad Request", HttpStatus.BAD_REQUEST));
        Optional<AuthUser> user = repository.getByIdAndNotDeleted(id);
        if (!user.isPresent()) return new DataDto<>(new AppErrorDto("Not Found", HttpStatus.NOT_FOUND));
        AuthDto authDto = mapper.toDto(user.get());
        System.out.println(authDto);
        return new DataDto<>(authDto);
    }


    @Override
    public DataDto<List<AuthDto>> getAll() {
        Role role = sessionUser.getRole();
        List<AuthUser> users = new ArrayList<>();
        if (role.equals(Role.ADMIN)) {
            Long orgId = sessionUser.getOrgId();
            users = repository.findAllByOrganizationIdAndDeletedFalse(orgId);
        }
        if (role.equals(Role.SUPER_ADMIN)) users = repository.findAllByDeletedFalse();
        return new DataDto<>(mapper.toDto(users));
    }


    @Override
    public DataDto<Boolean> update(AuthUpdateDto dto) {
        try {
            Optional<AuthUser> user = repository.getByIdAndNotDeleted(dto.getId());
            AuthUser authUser = mapper.updateFrom(dto, user.get());
            repository.save(authUser);
            return new DataDto<>(Boolean.TRUE);
        } catch (Exception e) {
            return new DataDto<>(Boolean.FALSE);
        }
    }

    @Override
    public DataDto<Boolean> delete(Long id) {
        if (Objects.isNull(id)) return new DataDto<>(new AppErrorDto("Bad Request", HttpStatus.BAD_REQUEST));
        Optional<AuthUser> user = repository.getByIdAndNotDeleted(id);
        if (!user.isPresent()) return new DataDto<>(new AppErrorDto("Not Found", HttpStatus.NOT_FOUND));
        String code = UUID.randomUUID().toString();
        repository.softDeleted(id, code);
        return new DataDto<>(Boolean.TRUE);
    }


    @Override
    public DataDto<List<AuthDto>> getWithCriteria(BaseCriteria criteria) throws SQLException {

        return null;
    }

    public DataDto<OtpResponse> loginByPhone(AuthUserPhoneDto loginDto) {
        String phoneNumber = String.format("+998%s", loginDto.phoneNumber);

        Optional<AuthUser> user = repository.findByPhoneNumberAndDeletedFalse(phoneNumber);

        if (user.isPresent()) {
            if (user.get().getStatus().equals(Status.ACTIVE)) {
                AuthUser authUser = user.get();
                OtpResponse response = otpService.send(loginDto);
                if (response.success) {
                    phoneCodeRepository.save(new PhoneCode(authUser.getPhoneNumber(), response.code.toString(), LocalDateTime.now().plusMinutes(otpProperties.getExpiration())));
                    return new DataDto<>(response);
                } else {
                    if (response.message.equals("Unauthorized")) {
                        return new DataDto<>(new AppErrorDto("MessageService is not working currently", "/auth/loginPhone", HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                    return new DataDto<>(new AppErrorDto(response.message, "/auth/loginPhone", HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } else return new DataDto<>(new AppErrorDto(HttpStatus.LOCKED, "/auth/loginPhone", "User not Active"));
        } else return new DataDto<>(new AppErrorDto(HttpStatus.NOT_FOUND, "User not found", "auth/loginPhone"));
    }

    public DataDto<SessionDto> confirmLoginCode(AuthUserCodePhoneDto dto) {
        AuthUserPasswordDto authUserPasswordDto = confirmCode(dto);
        if (Objects.nonNull(authUserPasswordDto)) return this.login(authUserPasswordDto);
        return new DataDto<>(new AppErrorDto(HttpStatus.BAD_REQUEST, "Incorrect Code entered", "/auth/confirmOtp"));

    }

    public AuthUserPasswordDto confirmCode(AuthUserCodePhoneDto dto) {
        String phoneNumber = String.format("+998%s", dto.phoneNumber);
        Optional<PhoneCode> phoneCodeOptional = phoneCodeRepository.findByPhoneNumberAndDeletedFalse(phoneNumber);
        if (!phoneCodeOptional.isPresent()) return null;
        PhoneCode phoneCode = phoneCodeOptional.get();
        long between = ChronoUnit.MINUTES.between(LocalDateTime.now(), phoneCode.getExpiration());
        if (phoneCode.getCode().equals(dto.code.toString()) && between >= 0)
            return new AuthUserPasswordDto(phoneNumber, phoneNumber, dto.deviceToken, dto.deviceId);
        return null;
    }


    public DataDto<SessionDto> confirmRegisterCode(AuthUserCodePhoneDto dto) {
        String phoneNumber = String.format("+998%s", dto.phoneNumber);
        AuthUserPasswordDto authUserPasswordDto = confirmCode(dto);
        if (Objects.nonNull(authUserPasswordDto)) {
            repository.save(new AuthUser(phoneNumber, passwordEncoder.encode(phoneNumber), Role.CLIENT, false));
            return this.login(authUserPasswordDto);
        }
        return new DataDto<>(new AppErrorDto(HttpStatus.BAD_REQUEST, "Incorrect Code entered", "/auth/confirmOtp"));
    }

    public DataDto<Boolean> block(Long id) {
        try {
            AuthUser authUser = blockUnblock(id);
            authUser.setStatus(Status.BLOCKED);
            repository.save(authUser);
            return new DataDto<>(Boolean.TRUE);
        } catch (Exception e) {
            if (e.getMessage().equals("FORBIDDEN"))
                return new DataDto<>(new AppErrorDto("FORBIDDEN", "/auth/block", HttpStatus.FORBIDDEN));
            if (e.getMessage().equals("NOT_ACTIVE"))
                return new DataDto<>(new AppErrorDto("Bad Request", "/auth/block", HttpStatus.BAD_REQUEST));
        }
        return new DataDto<>(Boolean.FALSE);
    }

    public AuthUser blockUnblock(Long id) {
        Optional<AuthUser> user = repository.getByIdAndNotDeleted(id);
        if (!sessionUser.getRole().equals(Role.ADMIN)) throw new RuntimeException("FORBIDDEN");
        Status status = sessionUser.getStatus();
        if (user.isPresent() && status.equals(Status.ACTIVE)) {
            return user.get();
        }
        throw new RuntimeException("NOT_ACTIVE");
    }

    public DataDto<Boolean> unblock(Long id) {
        try {
            AuthUser authUser = blockUnblock(id);
            authUser.setStatus(Status.ACTIVE);
            repository.save(authUser);
            return new DataDto<>(Boolean.TRUE);
        } catch (Exception e) {
            if (e.getMessage().equals("FORBIDDEN"))
                return new DataDto<>(new AppErrorDto("FORBIDDEN", "/auth/unblock", HttpStatus.FORBIDDEN));
            if (e.getMessage().equals("NOT_ACTIVE"))
                return new DataDto<>(new AppErrorDto("Bad Request", "/auth/unblock", HttpStatus.BAD_REQUEST));
        }
        return new DataDto<>(Boolean.FALSE);
    }
}
