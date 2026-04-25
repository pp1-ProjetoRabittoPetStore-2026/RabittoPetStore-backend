package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Orders;
import com.rabitto.backend.repositories.OrdersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrdersRepository repository;

    // GET /orders?status=Pendentes
    @GetMapping
    public List<Orders> listarPorStatus(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return repository.findByStatus(status);
        }
        return repository.findAll();
    }

    // POST /orders — criar novo pedido
    @PostMapping
    public Orders criar(@RequestBody Orders order) {
        return repository.save(order);
    }

    // PATCH /orders/{id}/{status} — atualizar status via path variable
    @PatchMapping("/{id}/{status}")
    public ResponseEntity<Orders> atualizarStatus(
            @PathVariable Long id,
            @PathVariable String status) {

        return repository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    return ResponseEntity.ok(repository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
