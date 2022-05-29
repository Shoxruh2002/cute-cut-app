package uz.pdp.cutecutapp.services.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.pdp.cutecutapp.criteria.BaseCriteria;
import uz.pdp.cutecutapp.dto.organization.OrganizationCreateDto;
import uz.pdp.cutecutapp.dto.organization.OrganizationDto;
import uz.pdp.cutecutapp.dto.organization.OrganizationUpdateDto;
import uz.pdp.cutecutapp.dto.responce.AppErrorDto;
import uz.pdp.cutecutapp.dto.responce.DataDto;
import uz.pdp.cutecutapp.entity.auth.AuthUser;
import uz.pdp.cutecutapp.entity.barbershop.BarberShop;
import uz.pdp.cutecutapp.entity.organization.Organization;
import uz.pdp.cutecutapp.enums.Status;
import uz.pdp.cutecutapp.mapper.organization.OrganizationMapper;
import uz.pdp.cutecutapp.repository.auth.AuthUserRepository;
import uz.pdp.cutecutapp.repository.barbershop.BarberShopRepository;
import uz.pdp.cutecutapp.repository.organization.OrganizationRepository;
import uz.pdp.cutecutapp.services.AbstractService;
import uz.pdp.cutecutapp.services.GenericCrudService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service

public class OrganizationService extends AbstractService<OrganizationRepository, OrganizationMapper>
        implements GenericCrudService<Organization, OrganizationDto, OrganizationCreateDto, OrganizationUpdateDto, BaseCriteria, Long> {

    public OrganizationService(OrganizationRepository repository, OrganizationMapper mapper) {
        super(repository, mapper);

    }

    @Autowired
    AuthUserRepository authUserRepository;

    @Autowired
    BarberShopRepository barberShopRepository;

    @Override
    public DataDto<Long> create(OrganizationCreateDto createDto) {
        Organization organization = mapper.fromCreateDto(createDto);
        Organization newOrg = repository.save(organization);
        return new DataDto<>(newOrg.getId(), HttpStatus.CREATED.value());
    }

    @Override
    public DataDto<Boolean> delete(Long id) {
        if (this.get(id).isSuccess()){
            repository.isDelete(id);
            return new DataDto<>(Boolean.TRUE,HttpStatus.NO_CONTENT.value());
        }
        return new DataDto<>(new AppErrorDto("Finding item not found with id : " + id, "/organization/delete", HttpStatus.NOT_FOUND));

    }

    @Override
    public DataDto<Boolean> update(OrganizationUpdateDto updateDto) {
        try {
            Optional<Organization> optionalOrganization = repository.findByIdAndDeletedFalse(updateDto.getId());
            Organization newOrganization = mapper.fromUpdate(updateDto, optionalOrganization.get());
            repository.save(newOrganization);
            return new DataDto<>(Boolean.TRUE,HttpStatus.OK.value());
        } catch (Exception e) {
            return new DataDto<>(Boolean.FALSE, HttpStatus.BAD_REQUEST.value());
        }
    }

    @Override
    public DataDto<List<OrganizationDto>> getAll() {
        List<Organization> organizationList = repository.findAllByDeletedFalse();
        List<OrganizationDto> organizationDtos = mapper.toDto(organizationList);
        return new DataDto<>(organizationDtos, HttpStatus.OK.value());
    }

    @Override
    public DataDto<OrganizationDto> get(Long id) {
        Optional<Organization> optionalOrganization = repository.findByIdAndDeletedFalse(id);
        if (optionalOrganization.isPresent()) {
            OrganizationDto organizationDto = mapper.toDto(optionalOrganization.get());
            return new DataDto<>(organizationDto,HttpStatus.OK.value());
        }else
            return new DataDto<>(new AppErrorDto("Finding item not found with id : "+id,"/organization/getById",HttpStatus.NOT_FOUND));
    }

    @Override
    public DataDto<List<OrganizationDto>> getWithCriteria(BaseCriteria criteria) throws SQLException {
        return null;
    }

    public DataDto<Boolean> block(OrganizationUpdateDto dto) {
        try {
            Optional<Organization> optionalOrganization = repository.findByIdAndDeletedFalse(dto.getId());
            Optional<AuthUser> optionalAuthUser = authUserRepository.findByAndOrganizationId(dto.getId());

            Organization organization = optionalOrganization.get();
            AuthUser authUser = optionalAuthUser.get();

            if (optionalOrganization.isPresent() && optionalAuthUser.isPresent() ){
                organization.setStatus(Status.BLOCKED);
                authUser.setStatus(Status.BLOCKED);
                authUserRepository.save(authUser);
                repository.save(organization);
                return new DataDto<>(Boolean.TRUE,HttpStatus.OK.value());
            }
            return new DataDto<>(new AppErrorDto("Finding item not found  ", HttpStatus.NOT_FOUND));

        } catch (Exception e) {
            return new DataDto<>(Boolean.FALSE, HttpStatus.BAD_REQUEST.value());
        }

    }

    public DataDto<Boolean> unblock(OrganizationUpdateDto dto) {
        try {
            Optional<Organization> optionalOrganization = repository.findByIdAndDeletedFalse(dto.getId());
            Optional<AuthUser> optionalAuthUser = authUserRepository.findByAndOrganizationId(dto.getId());

            Organization organization = optionalOrganization.get();
            AuthUser authUser = optionalAuthUser.get();

            if (optionalOrganization.isPresent() && optionalAuthUser.isPresent() ){
                organization.setStatus(Status.ACTIVE);
                authUser.setStatus(Status.ACTIVE);
                authUserRepository.save(authUser);
                repository.save(organization);
                return new DataDto<>(Boolean.TRUE,HttpStatus.OK.value());
            }
            return new DataDto<>(new AppErrorDto("Finding item not found  ", HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new DataDto<>(Boolean.FALSE, HttpStatus.BAD_REQUEST.value());
        }
    }
}
