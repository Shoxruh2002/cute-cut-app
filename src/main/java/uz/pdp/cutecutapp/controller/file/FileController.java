package uz.pdp.cutecutapp.controller.file;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.pdp.cutecutapp.controller.AbstractController;
import uz.pdp.cutecutapp.dto.file.UploadsDto;
import uz.pdp.cutecutapp.dto.responce.DataDto;
import uz.pdp.cutecutapp.services.file.FileStorageService;

import java.nio.file.NoSuchFileException;

/**
 * @author Axmadjonov Eliboy on Wed 12:32. 25/05/22
 * @project cute-cut-app
 */
@RestController
public class FileController extends AbstractController<FileStorageService> {

    protected FileController(FileStorageService service) {
        super(service);
    }

    @PostMapping(value = PATH + "/file/auth/uploadPicture",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<DataDto<String>> uploadPicture(@RequestParam MultipartFile file) {
        return new ResponseEntity<>(new DataDto<>(service.store(file)), HttpStatus.OK);
    }

    @GetMapping(value = PATH+"/file/auth/download/{name}")
    public ResponseEntity<DataDto<UploadsDto>> downloadFile(@PathVariable(name = "name") String name){
        try {
            return new ResponseEntity<>(new DataDto<>(service.loadResource(name)),HttpStatus.OK);
        } catch (NoSuchFileException e) {
            throw new RuntimeException(e);
        }
    }

}
