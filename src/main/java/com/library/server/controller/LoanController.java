package com.library.server.controller;

import com.library.server.dto.request.LoanRequestDTO;
import com.library.server.dto.request.RenewLoanRequestDTO;
import com.library.server.dto.request.ReturnBookRequestDTO;
import com.library.server.dto.response.LoanResponseDTO;
import com.library.server.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@CrossOrigin(origins = "*") // Chống lỗi CORS
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @GetMapping
    public List<LoanResponseDTO> getLoans() {
        // Gọi Service lấy dữ liệu thật
        return loanService.getAllLoansForDashboard();
    }

    @PostMapping
    public ResponseEntity<String> createLoan(@RequestBody @Valid LoanRequestDTO request) {
        try {
            loanService.createNewLoan(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Sửa dữ liệu!");
        } catch (Exception e) {
            // Nếu có lỗi (Sai ID, sách hết...), ném thông báo lỗi về Frontend
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> getLoanById(@PathVariable Integer id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLoansByUserId(@PathVariable Integer userId) {
        try {
            List<LoanResponseDTO> loans = loanService.getLoansByUserId(userId);

            if (loans.isEmpty()) {
                return ResponseEntity.ok("Độc giả này chưa có phiếu mượn nào.");
            }

            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLoan(@PathVariable Integer id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.ok("Đã xóa phiếu mượn thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @PutMapping("/details/{detailId}/renew")
    public ResponseEntity<String> renewLoanDetail(
            @PathVariable Integer detailId,
            @RequestBody RenewLoanRequestDTO request) {
        try {
            loanService.renewLoanDetail(detailId, request);
            return ResponseEntity.ok("Gia hạn thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/details/{detailId}/return")
    public ResponseEntity<String> returnBook(
            @PathVariable Integer detailId,
            @RequestBody(required = false) ReturnBookRequestDTO request) {
        try {
            String resultMessage = loanService.returnLoanDetail(detailId, request);

            // Gửi thẳng câu thông báo đó về cho màn hình alert của Frontend
            return ResponseEntity.ok(resultMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi trả sách: " + e.getMessage());
        }
    }

    @PostMapping("/from-reservation/{reservationId}")
    public ResponseEntity<String> createLoanFromReservation(@PathVariable Integer reservationId) {
        try {
            loanService.createLoanFromReservation(reservationId);
            return ResponseEntity.ok("Giao sách và tạo phiếu mượn thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/recent")
    public ResponseEntity<List<Object[]>> getRecentLoanDetails() {
        List<Object[]> recentLoans = loanService.getRecentLoansForTable();
        return ResponseEntity.ok(recentLoans);
    }
}