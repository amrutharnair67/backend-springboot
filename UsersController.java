package com.example.JavaTournament.controller;

import com.example.JavaTournament.entity.Users;
import com.example.JavaTournament.service.UsersService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")

public class UsersController {

    @Autowired
    private UsersService userService;

    // Endpoint for user registration
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> registerUser(
            @ModelAttribute Users user, // Populate all form fields, including 'teamLogo'
            BindingResult bindingResult) {

        // Check for validation errors on form data
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        // Check if user already exists
        if (userService.existsByEmail(user.getEmail())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "User is already registered. Please log in.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // Handle the MultipartFile and set it to the byte[] field
        if (user.getTeamLogo() != null && !user.getTeamLogo().isEmpty()) {
            try {
                byte[] fileData = user.getTeamLogo().getBytes();
                user.setTeamLogoBytes(fileData); // Save byte[] to entity
            } catch (IOException e) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Failed to read file data: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }

        // Save the user to the database
        userService.saveUser(user);

        // Return success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful.");
        return ResponseEntity.ok(response);
    }



    // Endpoint for user login
//    @PostMapping("/login")
//    public ResponseEntity<String> loginUser(@RequestBody Users user) {
//        if (user.getEmail() == null || user.getEmail().isEmpty()) {
//            return ResponseEntity.badRequest().body("Email is required.");
//        }
//
//        Users existingUser = userService.findByEmail(user.getEmail());
//
//        if (existingUser != null && existingUser.getPassword().equals(user.getPassword())) {
//            return ResponseEntity.ok("Login successful.");
//        } else {
//            return ResponseEntity.badRequest().body("Invalid email or password.");
//        }
//    }
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Users user) {
        Map<String, String> response = new HashMap<>();

        // Validate email
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("error", "Email is required.");
            return ResponseEntity.badRequest().body(response);
        }

        // Fetch the existing user by email
        Users existingUser = userService.findByEmail(user.getEmail());

        // Check if the user exists
        if (existingUser == null) {
            response.put("error", "Email does not exist."); // Email not found error
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Validate password
        if (!existingUser.getPassword().equals(user.getPassword())) {
            response.put("error", "Invalid password."); // Incorrect password error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Successful login
        response.put("message", "Login successful.");
        response.put("teamName", existingUser.getteamname()); // Include team name in the response
        return ResponseEntity.ok(response);
    }


//    // Endpoint for forgot password
//    @PostMapping("/forgotpassword")
//    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> requestBody) {
//        String email = requestBody.get("email");
//        if (email == null || email.trim().isEmpty()) {
//            return ResponseEntity.badRequest().body("Email must not be empty.");
//        }
//
//        if (userService.existsByEmail(email)) {
//            String token = userService.generateResetPasswordToken(email);
//            if (token != null) {
//                return ResponseEntity.ok("Reset token has been sent to your email."+ token);
//            } else {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate reset token.");
//            }
//        } else {
//            return ResponseEntity.badRequest().body("Email does not exist.");
//        }
//    }

    @PostMapping("/forgotpassword")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        Map<String, String> response = new HashMap<>();

        if (email == null || email.trim().isEmpty()) {
            response.put("message", "Email must not be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        if (userService.existsByEmail(email)) {
            String token = userService.generateResetPasswordToken(email);
            if (token != null) {
                // Construct the reset password link
                String resetLink = "http://localhost:3000/resetpassword?token=" + token;

                response.put("message", "Reset token has been sent to your email.");
                response.put("resetLink", resetLink); // Include the reset link
                response.put("token", token); // Include the token if needed
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Failed to generate reset token.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            response.put("message", "Email does not exist.");
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/resetpassword")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");
        String newPassword = requestBody.get("newPassword");
        Map<String, String> response = new HashMap<>();

        if (token == null || newPassword == null) {
            response.put("message", "Token and new password must not be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = userService.resetPassword(token, newPassword);
        if (success) {
            response.put("message", "Password has been reset successfully."); // JSON response
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Invalid or expired token."); // JSON response
            return ResponseEntity.badRequest().body(response);
        }
    }
}