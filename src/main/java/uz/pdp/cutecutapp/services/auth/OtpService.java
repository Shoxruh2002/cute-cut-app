package uz.pdp.cutecutapp.services.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.pdp.cutecutapp.dto.auth.AuthUserPhoneDto;
import uz.pdp.cutecutapp.dto.otp.OtpResponse;
import uz.pdp.cutecutapp.properties.OtpProperties;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Service
public class OtpService {

    private final OtpProperties otpProperties;

    private final ObjectMapper objectMapper;

    public OtpService(OtpProperties otpProperties, ObjectMapper objectMapper) {
        this.otpProperties = otpProperties;
        this.objectMapper = objectMapper;
    }

    public OtpResponse send(AuthUserPhoneDto dto) {
        try {
//            HttpClient httpClient = HttpClientBuilder.create().build();
//            String url = otpProperties.getApi();
            Random random = SecureRandom.getInstanceStrong();
            int code = otpProperties.getFrom() + random.nextInt(otpProperties.getBound());
//            OtpDto otpDto = new OtpDto(dto.phoneNumber, otpProperties.getOriginator(),
//                    (otpProperties.getBody() + code));
//            HttpPost httpPost = new HttpPost(url);
//            httpPost.addHeader("Authorization", otpProperties.getAccessKey());
//            httpPost.addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
//            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(otpDto)));
//
//
//            HttpResponse response = httpClient.execute(httpPost);

//            if (response.getStatusLine().getStatusCode() == HttpStatus.CREATED.value()) {
            return new OtpResponse(code, dto.phoneNumber);
//            } else
//                return new OtpResponse(false, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());

        } catch (NoSuchAlgorithmException e) {
            return new OtpResponse(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

}