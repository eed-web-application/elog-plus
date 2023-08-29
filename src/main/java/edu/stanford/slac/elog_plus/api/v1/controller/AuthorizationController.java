package edu.stanford.slac.elog_plus.api.v1.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class AuthorizationController {
    public void me(@RequestHeader() String authenticatedUser) {

    }
}
