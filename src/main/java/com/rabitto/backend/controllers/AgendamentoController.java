package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    // Lista todos os agendamentos salvos no banco
    @GetMapping("/status/{status}")
    public List<Agendamento> listar(@PathVariable String status) {
        if (status != null && !status.isEmpty()) {
            return agendamentoRepository.findByStatus(status);
        }
        return agendamentoRepository.findAll();
    }

    // Rota para o aplicativo consultar quais horários estão livres em um dia
    // específico
    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<?> buscarHorariosDisponiveis(
            @RequestParam LocalDate data,
            @RequestParam Long servicoId) {

        // 1. Busca o serviço pra saber se a regra é de Banhista ou Veterinário
        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (servico == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }

        boolean isVeterinario = servico.getNome().toLowerCase().contains("consulta") ||
                servico.getNome().toLowerCase().contains("vacina");

        // 2. Define o período do expediente daquele dia (09:00 às 17:00)
        LocalDateTime inicioExpediente = data.atTime(9, 0);
        LocalDateTime fimExpediente = data.atTime(17, 0);

        // 3. Puxa do banco tudo que já está ocupado nesse intervalo
        List<Agendamento> agendamentosDoDia = agendamentoRepository.findByDataHoraBetween(inicioExpediente,
                fimExpediente);

        List<String> horariosLivres = new ArrayList<>();

        if (isVeterinario) {
            // Regra do VET: Slots de 30 min, limite de 1 profissional
            for (int hora = 9; hora < 17; hora++) {
                for (int min : new int[] { 0, 30 }) {
                    LocalDateTime horarioVerificado = data.atTime(hora, min);

                    long ocupados = agendamentosDoDia.stream()
                            .filter(a -> a.getDataHora().equals(horarioVerificado))
                            .filter(a -> a.getServico().getNome().toLowerCase().contains("consulta") ||
                                    a.getServico().getNome().toLowerCase().contains("vacina"))
                            .count();

                    if (ocupados < 1) {
                        horariosLivres.add(String.format("%02d:%02d", hora, min));
                    }
                }
            }
        } else {
            // Regra do BANHO: Slots de 1h, limite de 2 profissionais
            for (int hora = 9; hora < 17; hora++) {
                LocalDateTime horarioVerificado = data.atTime(hora, 0);

                long ocupados = agendamentosDoDia.stream()
                        .filter(a -> a.getDataHora().equals(horarioVerificado))
                        .filter(a -> !(a.getServico().getNome().toLowerCase().contains("consulta") ||
                                a.getServico().getNome().toLowerCase().contains("vacina")))
                        .count();

                if (ocupados < 2) {
                    horariosLivres.add(String.format("%02d:00", hora));
                }
            }
        }

        return ResponseEntity.ok(horariosLivres);
    }

    // Salva um agendamento novo aplicando as travas de segurança
    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Agendamento agendamento) {
        LocalDateTime dataHora = agendamento.getDataHora();

        // VALIDAÇÃO 1: Horário de funcionamento (09h às 17h)
        if (dataHora.getHour() < 9 || dataHora.getHour() >= 17) {
            return ResponseEntity.badRequest().body("Erro: O Rabitto funciona apenas das 09:00 às 17:00.");
        }

        Servico servicoEscolhido = servicoRepository.findById(agendamento.getServico().getId()).orElse(null);
        if (servicoEscolhido == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }

        String nomeServico = servicoEscolhido.getNome().toLowerCase();
        boolean isVeterinario = nomeServico.contains("consulta") || nomeServico.contains("vacina");

        // Busca agendamentos que já existem exatamente na mesma hora
        List<Agendamento> agendamentosNoHorario = agendamentoRepository.findByDataHora(dataHora);

        int contagemVet = 0;
        int contagemBanho = 0;

        for (Agendamento agendado : agendamentosNoHorario) {
            Servico servicoAgendado = servicoRepository.findById(agendado.getServico().getId()).orElse(null);
            if (servicoAgendado != null) {
                String nomeAgendado = servicoAgendado.getNome().toLowerCase();
                if (nomeAgendado.contains("consulta") || nomeAgendado.contains("vacina")) {
                    contagemVet++;
                } else {
                    contagemBanho++;
                }
            }
        }

        // VALIDAÇÃO 2: Limites de funcionários e duração dos slots
        if (isVeterinario) {
            if (contagemVet >= 1) {
                return ResponseEntity.badRequest().body("Erro: O veterinário já está ocupado nesse horário.");
            }
            if (dataHora.getMinute() != 0 && dataHora.getMinute() != 30) {
                return ResponseEntity.badRequest()
                        .body("Erro: Consultas devem ser em intervalos de 30 min (ex: 14:00, 14:30).");
            }
        } else {
            if (contagemBanho >= 2) {
                return ResponseEntity.badRequest().body("Erro: Todos os banhistas já estão ocupados nesse horário.");
            }
            if (dataHora.getMinute() != 0) {
                return ResponseEntity.badRequest().body("Erro: Banhos devem ser marcados em hora cheia (ex: 10:00).");
            }
        }

        // Se passar por tudo, salva no banco e devolve o agendamento (com status
        // "Pendente")
        return ResponseEntity.ok(agendamentoRepository.save(agendamento));
    }

    // Atualiza um agendamento (Ex: Gerente aprova mudando o status)
    @PutMapping("/{id}")
    public Agendamento atualizar(@PathVariable Long id, @RequestBody Agendamento agendamentoAtualizado) {
        agendamentoAtualizado.setId(id);
        return agendamentoRepository.save(agendamentoAtualizado);
    }

    // Atualiza apenas o status de um agendamento (fluxo do gerente)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String novoStatus = body.get("status");
        if (novoStatus == null || novoStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Erro: campo 'status' é obrigatório.");
        }

        List<String> statusPermitidos = List.of("Pendente", "Aguardando", "Em Serviço", "Pronto");
        if (!statusPermitidos.contains(novoStatus)) {
            return ResponseEntity.badRequest().body("Erro: status inválido. Use: " + statusPermitidos);
        }

        return agendamentoRepository.findById(id)
                .map(agendamento -> {
                    agendamento.setStatus(novoStatus);
                    return ResponseEntity.ok(agendamentoRepository.save(agendamento));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Remove um agendamento do banco
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        agendamentoRepository.deleteById(id);
    }
}