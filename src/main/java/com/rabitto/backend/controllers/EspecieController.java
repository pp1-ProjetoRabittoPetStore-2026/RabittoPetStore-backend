package com.rabitto.backend.controllers;

import com.rabitto.backend.dto.EspecieDTO;
import com.rabitto.backend.models.Especie;
import com.rabitto.backend.models.Porte;
import com.rabitto.backend.services.EspecieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/especies")
public class EspecieController {

    @Autowired
    private EspecieService especieService;

    @GetMapping
    public List<EspecieDTO> listarTodas() {
        return especieService.getRacasPorEspecie().entrySet().stream()
                .map(entry -> new EspecieDTO(
                        entry.getKey().name(),
                        entry.getKey().getDisplayName(),
                        entry.getValue()))
                .toList();
    }

    @GetMapping("/{especie}/racas")
    public List<String> listarRacas(@PathVariable Especie especie) {
        return especieService.getRacas(especie);
    }

    @GetMapping("/portes")
    public List<String> listarPortes() {
        return List.of(
                Porte.PEQUENO.getDisplayName().toLowerCase(),
                Porte.MEDIO.getDisplayName().toLowerCase(),
                Porte.GRANDE.getDisplayName().toLowerCase()
        );
    }
}
