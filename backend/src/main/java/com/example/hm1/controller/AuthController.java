package com.example.hm1.controller;

import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.dao.RoleRepository;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import com.example.hm1.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API для аутентифікації та реєстрації користувачів")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepo customerRepository;
    private final @Lazy PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, RoleRepository roleRepository, UserRepository userRepository, CustomerRepo customerRepository, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @Operation(summary = "Реєстрація нового користувача", description = "Створює нового користувача з роллю USER та пов'язаний обліковий запис Customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Користувач успішно зареєстрований"),
            @ApiResponse(responseCode = "400", description = "Помилка реєстрації: користувач з таким ім'ям або email вже існує, або невалідні дані")
    })
    public ResponseEntity<?> register(
            @Parameter(description = "Дані для реєстрації: username, email, password", required = true)
            @RequestBody RegisterRequest req) {
        try {
            // Перевіряємо чи користувач вже існує
            if (userRepository.existsByUsername(req.getUsername())) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            // Перевіряємо чи email вже існує
            if (customerRepository.findByEmail(req.getEmail()) != null) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // Отримуємо роль USER
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

            // Створюємо користувача
            User user = User.builder()
                    .username(req.getUsername())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .enabled(true)
                    .roles(Set.of(userRole))
                    .build();

            user = userRepository.save(user);
            
            // Перевіряємо, що user має ID після збереження
            if (user.getId() == null) {
                throw new RuntimeException("User ID is null after save");
            }
            
            System.out.println("AuthController: User saved with ID: " + user.getId());

            // Створюємо клієнта
            Customer customer = new Customer(req.getUsername(), req.getEmail(), 25); // Вік за замовчуванням
            customer.setUser(user);
            Customer savedCustomer = customerRepository.save(customer);
            
            System.out.println("AuthController: Customer saved with ID: " + savedCustomer.getId() + ", user ID: " + 
                    (savedCustomer.getUser() != null ? savedCustomer.getUser().getId() : "null"));

            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизація користувача", description = "Перевіряє облікові дані користувача та повертає JWT токен для доступу до API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успішна авторизація, повертається JWT токен та ролі користувача"),
            @ApiResponse(responseCode = "401", description = "Помилка авторизації: невірний username або password, або користувач не знайдений")
    })
    public ResponseEntity<?> login(
            @Parameter(description = "Облікові дані: username та password", required = true)
            @RequestBody LoginRequest req) {
        try {
            // Перевіряємо чи користувач існує
            if (!userRepository.existsByUsername(req.getUsername())) {
                Map<String, String> error = new java.util.HashMap<>();
                error.put("message", "Користувач не знайдений. Будь ласка, зареєструйтеся.");
                error.put("error", "USER_NOT_FOUND");
                return ResponseEntity.status(401).body(error);
            }

            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String[] roles = user.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toArray(String[]::new);

            String token = jwtService.generateToken(user.getUsername(), roles);
            return ResponseEntity.ok(new AuthResponse(token, roles));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", "Невірний пароль. Спробуйте ще раз.");
            error.put("error", "BAD_CREDENTIALS");
            return ResponseEntity.status(401).body(error);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", "Помилка входу: " + e.getMessage());
            error.put("error", "LOGIN_FAILED");
            return ResponseEntity.status(400).body(error);
        }
    }

    public static class RegisterRequest {
        @NotBlank
        private String username;
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;

        public RegisterRequest() {
        }

        public @NotBlank String getUsername() {
            return this.username;
        }

        public @Email @NotBlank String getEmail() {
            return this.email;
        }

        public @NotBlank String getPassword() {
            return this.password;
        }

        public void setUsername(@NotBlank String username) {
            this.username = username;
        }

        public void setEmail(@Email @NotBlank String email) {
            this.email = email;
        }

        public void setPassword(@NotBlank String password) {
            this.password = password;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof RegisterRequest)) return false;
            final RegisterRequest other = (RegisterRequest) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$username = this.getUsername();
            final Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
            final Object this$email = this.getEmail();
            final Object other$email = other.getEmail();
            if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
            final Object this$password = this.getPassword();
            final Object other$password = other.getPassword();
            if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof RegisterRequest;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            final Object $email = this.getEmail();
            result = result * PRIME + ($email == null ? 43 : $email.hashCode());
            final Object $password = this.getPassword();
            result = result * PRIME + ($password == null ? 43 : $password.hashCode());
            return result;
        }

        public String toString() {
            return "AuthController.RegisterRequest(username=" + this.getUsername() + ", email=" + this.getEmail() + ", password=" + this.getPassword() + ")";
        }
    }

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public LoginRequest() {
        }

        public @NotBlank String getUsername() {
            return this.username;
        }

        public @NotBlank String getPassword() {
            return this.password;
        }

        public void setUsername(@NotBlank String username) {
            this.username = username;
        }

        public void setPassword(@NotBlank String password) {
            this.password = password;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof LoginRequest)) return false;
            final LoginRequest other = (LoginRequest) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$username = this.getUsername();
            final Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
            final Object this$password = this.getPassword();
            final Object other$password = other.getPassword();
            if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof LoginRequest;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            final Object $password = this.getPassword();
            result = result * PRIME + ($password == null ? 43 : $password.hashCode());
            return result;
        }

        public String toString() {
            return "AuthController.LoginRequest(username=" + this.getUsername() + ", password=" + this.getPassword() + ")";
        }
    }

    public static class AuthResponse {
        private final String accessToken;
        private final String[] roles;

        public AuthResponse(String accessToken, String[] roles) {
            this.accessToken = accessToken;
            this.roles = roles;
        }

        public String getAccessToken() {
            return this.accessToken;
        }

        public String[] getRoles() {
            return this.roles;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof AuthResponse)) return false;
            final AuthResponse other = (AuthResponse) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$accessToken = this.getAccessToken();
            final Object other$accessToken = other.getAccessToken();
            if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof AuthResponse;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $accessToken = this.getAccessToken();
            result = result * PRIME + ($accessToken == null ? 43 : $accessToken.hashCode());
            return result;
        }

        public String toString() {
            return "AuthController.AuthResponse(accessToken=" + this.getAccessToken() + ")";
        }
    }
}