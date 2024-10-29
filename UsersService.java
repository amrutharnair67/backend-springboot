package com.example.JavaTournament.service;

import com.example.JavaTournament.entity.Users;
import com.example.JavaTournament.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Method to check if a user exists by email
    public boolean existsByEmail(String email) {
        return usersRepository.findByEmail(email) != null;
    }

    // Method to generate a reset password token and send an email
    public String generateResetPasswordToken(String email) {
        Users user = usersRepository.findByEmail(email);
        if (user != null) {
            try {
                String token = UUID.randomUUID().toString(); // Generate a unique token
                user.setResetPasswordToken(token);
                usersRepository.save(user); // Save the user with the new token

                // Send the token to the user's email
                sendResetPasswordEmail(user.getEmail(), token);
                return token; // Return the token on success
            } catch (Exception e) {
                // Log the error for debugging
                System.err.println("Error generating reset token: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace for debugging
                return null; // Return null to indicate failure
            }
        }
        return null; // Return null if the user does not exist
    }



    // Send the reset password email using SMTP
    private void sendResetPasswordEmail(String email, String token) {
        String resetLink = "http://localhost:3000/resetpassword?token=" + token; // Replace with your actual frontend URL
        String subject = "Password Reset Request";
        String body = "Click the link below to reset your password:\n" + resetLink + "\n\nIf you did not request this, please ignore this email.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("your-email@gmail.com"); // Replace with your email

        mailSender.send(message);
        System.out.println("Reset password email sent successfully to " + email);
    }

    public boolean resetPassword(String token, String newPassword) {
        System.out.println("Resetting password for token: " + token); // Debug statement
        Users user = usersRepository.findByResetPasswordToken(token);
        if (user != null) {
            System.out.println("User found for token: " + user.getEmail()); // Debug statement
            user.setPassword(newPassword);
            user.setResetPasswordToken(null); // Clear the reset token after password is reset
            usersRepository.save(user);
            return true;
        }
        System.out.println("No user found for the token."); // Debug statement
        return false;
    }



    // Additional methods
    public Users findByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    public void saveUser(Users user) {
        usersRepository.save(user);
    }
    public String saveFile(MultipartFile file) {
        // Define the location where files will be saved
        String directoryPath = "path/to/upload/directory/";

        // Get the original filename of the uploaded file
        String fileName = file.getOriginalFilename();

        // Create a Path object that points to the file's destination
        Path filePath = Paths.get(directoryPath + fileName);

        try {
            // Create the directory if it does not exist
            Files.createDirectories(filePath.getParent());

            // Save the file to the specified location
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the file path as a string for further processing if needed
            return filePath.toString();
        } catch (IOException e) {
            // Log the error message for debugging
            e.printStackTrace();

            // Throw a custom exception with a meaningful message
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }
}

