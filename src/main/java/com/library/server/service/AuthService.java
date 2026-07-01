package com.library.server.service;

import com.library.server.dto.request.RegisterRequestDTO;
import com.library.server.dto.request.LoginRequestDTO;
import com.library.server.dto.response.LoginResponseDTO;
import com.library.server.dto.response.UserDTO;
import com.library.server.entity.Role;
import com.library.server.entity.User;
import com.library.server.repository.RoleRepository;
import com.library.server.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public String register(RegisterRequestDTO request) {
        // Validate password
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        
        // Có thể thêm validate độ dài mật khẩu nếu muốn
        if (request.getPassword().length() < 6) {
             throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng. Vui lòng sử dụng email khác!");
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new RuntimeException("Số điện thoại này đã được đăng ký. Vui lòng sử dụng số khác!");
            }
        }

        Role userRole = roleRepository.findByName("user")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy quyền mặc định (user)."));

        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setPhone(request.getPhone());
        newUser.setRole(userRole);
        newUser.setStatus("ACTIVE");

        userRepository.save(newUser);
        return "Đăng ký tài khoản thành công!";
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            // 1. Cố gắng xác thực người dùng
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // Lỗi sai mật khẩu hoặc email không tồn tại trong hệ thống xác thực
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
        } catch (AuthenticationException e) {
            // Các lỗi xác thực khác (tài khoản bị khóa, hết hạn, v.v.)
            throw new RuntimeException("Xác thực thất bại: " + e.getMessage());
        }

        // 2. Nếu đi xuống được đây tức là đã đăng nhập thành công
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy tài khoản sau khi xác thực!"));

        // 3. Kiểm tra trạng thái tài khoản (Active/Inactive)
        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }

        // 4. Sinh Token
        String jwtToken = jwtService.generateToken(user);

        // 5. Build DTO
        UserDTO userDto = UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getName() : "NO_ROLE")
                .msv(user.getMsv())
                .build();

        return LoginResponseDTO.builder()
                .message("Đăng nhập thành công!")
                .token(jwtToken)
                .user(userDto)
                .build();
    }
}