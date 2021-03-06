package com.codeback.web.controller;
import com.codeback.domain.jwt.JwtFilter;
import com.codeback.domain.jwt.TokenProvider;
import com.codeback.domain.user.User;
import com.codeback.service.email.EmailService;
import com.codeback.service.user.UserService;
import com.codeback.util.RedisUtill;
import com.codeback.util.SecurityCipher;
import com.codeback.web.dto.*;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.Struct;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

/**
 * @Author: TCMALTUNKAN - MEHMET ANIL ALTUNKAN
 * @Date: 30.12.2019:09:52, Pzt
 **/
@RestController
@RequestMapping("/auth")
public class AuthController {


    private final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisUtill redisUtill;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${signUpCookieName}")
    private String signUpCookieName;

    public AuthController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    // ????????? ??? ?????? ??????
    @PostMapping("/login")
    @ApiOperation(value = "????????? ??? ??????", notes = "????????? ??? ?????? ????????? ?????? ??? ????????? ?????? ??????", response = TokenDto.class)
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginDto
    ){

        Optional<User> userOptional = userService.findUserByEmail(loginDto.getEmail());

        // ????????? ????????? 401 RETURN
        if(!userOptional.isPresent()){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // ???????????? ????????? ????????? 401 RETURN
        User user = userOptional.get();

        // id,passoword??? ?????? UsernamePasswordAuthenticationToken??? ??????
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(), loginDto.getPassword());

        // ???????????? authenticate ???????????? CustomUserDetailsService??? ?????? loadUserByUsername ??????
        // ??? ??????????????? authentication?????? ???????????? ?????? SecurityContext??? ??????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);



        HttpHeaders responseHeaders = new HttpHeaders();
        TokenDto newAccessToken;
        TokenDto newRefreshToken;

        newAccessToken = tokenProvider.generateAccessToken(user.getUserNumber());
        newRefreshToken = tokenProvider.generateRefreshToken(user.getUserNumber());
        userService.addAccessTokenCookie(responseHeaders, newAccessToken);
        userService.addRefreshTokenCookie(responseHeaders, newRefreshToken);

        // redis??? refresh?????? ??????
        redisUtill.setData(newRefreshToken.getTokenValue(),newRefreshToken.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyyMMddhhmmss")));

        LoginResponseDto loginResponse = new LoginResponseDto(LoginResponseDto.SuccessFailure.SUCCESS, "Auth successful. Tokens are created in cookie.");
        return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
    }




    @GetMapping(value = "/duplicate/email/{email}")
    public ResponseEntity<?> duplicateEmailCheck(@PathVariable String email) {
        Optional<User> user = userService.findUserByEmail(email);
        System.out.println(email);
        if(!user.isPresent())
            return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/duplicate/nickname/{nickname}")
    public ResponseEntity<?> duplicateNickNameCheck(@PathVariable String nickname) {
        Optional<User> user = userService.findUserByNickname(nickname);
        if(!user.isPresent())
            return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PostMapping(value = "/email/req")
    public ResponseEntity<?> emailAuth(HttpServletRequest request, @RequestBody EmailAuthRequestDto requestDto) throws MessagingException {
        String email = requestDto.getEmail();
        Optional<User> user = userService.findUserByEmail(email);


        // 6????????? ?????? ??? ??????
        StringBuffer emailcontent = new StringBuffer();
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();
        int num = 0;

        while(buffer.length() < 6) {
            num = random.nextInt(10);
            buffer.append(num);
        }

        String auth_code = buffer.toString();

        // ????????? ????????? redis??? key: email value:auth code??? ??????
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        vop.set(email, auth_code);

        emailcontent.append("<!DOCTYPE html>");
        emailcontent.append("<html>");
        emailcontent.append("<head>");
        emailcontent.append("</head>");
        emailcontent.append("<body>");
        emailcontent.append(
                " <div" 																																																	+
                        "	style=\"font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 400px; height: 600px; border-top: 4px solid #DF01A5; margin: 100px auto; padding: 30px 0; box-sizing: border-box;\">"		+
                        "	<h1 style=\"margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;\">"																															+
                        "		<span style=\"font-size: 15px; margin: 0 0 10px 3px;\">CODE BACK</span><br />"																													+
                        "		<span style=\"color: #FA58D0\">????????????</span> ???????????????."																																				+
                        "	</h1>\n"																																																+
                        "	<p style=\"font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;\">"																																													+
                        "		codeback??? ????????? ????????? ???????????? ??????????????????.<br />"																																						+
                        "		?????? <b style=\"color: #FA58D0\">'?????? ??????'</b> ????????? ?????? ????????? ??????????????? ????????? ?????????.<br />"																													+
                        "		???????????????."																																															+
                        "	</p>"																																																	+
                        "		<p"																																																	+
                        "			style=\"display: inline-block; width: 210px; height: 45px; margin: 30px 5px 40px; background: #F6CEEC; line-height: 45px; vertical-align: middle; font-size: 16px;\">"							+
                        "			code : " +auth_code +"</p>"																																														+
                        "	</a>"																																																	+
                        "	<div style=\"border-top: 1px solid #DF01A5; padding: 5px;\"></div>"																																		+
                        " </div>"
        );
        emailcontent.append("</body>");
        emailcontent.append("</html>");
        emailService.sendMail(email, "[CodeBack] ???????????? ?????? ??????", emailcontent.toString());
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Email Auth Confirm", notes = "????????? ?????? ??????????????? ????????? ??????")
    @PostMapping(value = "/email/confirm")
    public ResponseEntity<?> emailConfirm(HttpServletRequest request, @RequestBody EmailAuthConfirmDto requestDto) {
        String code = requestDto.getCode();
        String email = requestDto.getEmail();

        Optional<User> user = userService.findUserByEmail(email);

        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        String storedCode = vop.get(email);



        // Redis??? ????????? ??????????????? ???????????? ?????? ??????
        if(storedCode.equals(code)){
            return new ResponseEntity<String>("true", HttpStatus.OK);
        }
        else{ // ?????? ??????
            return new ResponseEntity<String>("false", HttpStatus.BAD_REQUEST);

        }
    }

    //----------------???????????? ????????? ????????? ?????? ??????
    @PostMapping(value = "/emailWithPassword/req")
    public ResponseEntity<?> emailAuthWithPassword(HttpServletRequest request, @RequestBody EmailAuthRequestDto requestDto) throws MessagingException {
        String email = requestDto.getEmail();
        Optional<User> user = userService.findUserByEmail(email);



        // 6????????? ?????? ??? ??????
        StringBuffer emailcontent = new StringBuffer();
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();
        int num = 0;

        while(buffer.length() < 6) {
            num = random.nextInt(10);
            buffer.append(num);
        }

        String auth_code = buffer.toString();

        // ????????? ????????? redis??? key: email value:auth code??? ??????
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        vop.set(email, auth_code);

        emailcontent.append("<!DOCTYPE html>");
        emailcontent.append("<html>");
        emailcontent.append("<head>");
        emailcontent.append("</head>");
        emailcontent.append("<body>");
        emailcontent.append(
                " <div" 																																																	+
                        "	style=\"font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 400px; height: 600px; border-top: 4px solid #DF01A5; margin: 100px auto; padding: 30px 0; box-sizing: border-box;\">"		+
                        "	<h1 style=\"margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;\">"																															+
                        "		<span style=\"font-size: 15px; margin: 0 0 10px 3px;\">CODE BACK</span><br />"																													+
                        "		<span style=\"color: #FA58D0\">????????????</span> ???????????????."																																				+
                        "	</h1>\n"																																																+
                        "	<p style=\"font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;\">"																																													+
                        "		??????????????? ???????????? ????????? ?????? ?????? ???????????????.<br />"																																						+
                        "		?????? <b style=\"color: #FA58D0\">'?????? ??????'</b> ????????? ?????? ????????? ??????????????? ??????????????????.<br />"																													+
                        "		???????????????."																																															+
                        "	</p>"																																																	+
                        "		<p"																																																	+
                        "			style=\"display: inline-block; width: 210px; height: 45px; margin: 30px 5px 40px; background: #F6CEEC; line-height: 45px; vertical-align: middle; font-size: 16px;\">"							+
                        "			code : " +auth_code +"</p>"																																														+
                        "	</a>"																																																	+
                        "	<div style=\"border-top: 1px solid #DF01A5; padding: 5px;\"></div>"																																		+
                        " </div>"
        );
        emailcontent.append("</body>");
        emailcontent.append("</html>");
        emailService.sendMail(email, "[CodeBack] ???????????? ?????? ??????", emailcontent.toString());
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Email Auth Confirm WithPassword", notes = "????????? ?????? ??????????????? ????????? ???????????? ????????? ????????? ?????? ??????")
    @PostMapping(value = "/emailWithPassword/confirm")
    public ResponseEntity<?> emailConfirmWithPassword(HttpServletRequest request, @RequestBody EmailAuthConfirmDto requestDto) {
        String code = requestDto.getCode();
        String email = requestDto.getEmail();

        Optional<User> user = userService.findUserByEmail(email);

        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        String storedCode = vop.get(email);


        // Redis??? ????????? ??????????????? ???????????? ?????? ??????
        if(storedCode.equals(code)){
            ResponseEntity<?> res = userService.addEmailCookie();
            return res;
        }
        else{ // ?????? ??????
            return new ResponseEntity<String>("false",HttpStatus.OK);

        }
    }

    //------------------????????????????????? ????????? ?????? ???

    @ApiOperation(value = "???????????????????????? ??????", notes = "???????????? ??? ????????? ???????????? ???????????? ????????? ????????????.")
    @GetMapping("/startsignup")
    public ResponseEntity<?> signupPage(){
        try {
            ResponseEntity<?> res = userService.addSignUpCookie();
            return res; //duration : 300sec
        }
        catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "????????????", notes = "????????????")
    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        try {
            Cookie accessToken = new Cookie("accessToken", null);
            Cookie refreshToken = new Cookie("refreshToken", null);
            accessToken.setMaxAge(0); // ????????? expiration ????????? 0?????? ?????? ?????????.
            refreshToken.setMaxAge(0); // ????????? expiration ????????? 0?????? ?????? ?????????.
            accessToken.setPath("/");
            refreshToken.setPath("/"); // ?????? ???????????? ?????? ????????? ?????????.
            response.addCookie(accessToken);
            response.addCookie(refreshToken);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}