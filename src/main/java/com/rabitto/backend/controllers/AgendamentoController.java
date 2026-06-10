package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.FuncionarioRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import com.rabitto.backend.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private static final List<String> STATUS_PERMITIDOS =
            List.of("Pendente", "Aguardando", "Em Serviço", "Pronto", "Rejeitado", "Cancelado", "Confirmado");

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private JwtService jwtService;

    // Lista todos os agendamentos salvos no banco (opcionalmente por status)
    @GetMapping(value = { "/status", "/status/{status}" })
    public List<Agendamento> listar(@PathVariable(required = false) String status) {
        List<Agendamento> lista = (status != null && !status.isEmpty())
                ? agendamentoRepository.findByStatus(status)
                : agendamentoRepository.findAll();
        lista.forEach(this::sanitize);
        return lista;
    }

    // Agenda do veterinario logado: apenas as consultas dele, em ordem cronologica
    @GetMapping("/vet/agenda")
    public List<Agendamento> agendaVet(
            @RequestHeader(value = "Authorization", required = false) String header,
            @RequestParam(required = false) LocalDate data) {
        Long vetId = jwtService.extractUid(header);
        LocalDate dia = data != null ? data : LocalDate.now();
        LocalDateTime inicio = dia.atTime(0, 0);
        LocalDateTime fim = dia.atTime(23, 59);

        List<Agendamento> agenda = agendamentoRepository
                .findByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(vetId, inicio, fim)
                .stream()
                .filter(a -> isVetServico(a.getServico()))
                .collect(Collectors.toList());
        agenda.forEach(this::sanitize);
        return agenda;
    }

    // Agendamentos do tutor logado (todos os seus pets), em ordem cronologica
    @GetMapping("/meus")
    public List<Agendamento> meusAgendamentos(
            @RequestHeader(value = "Authorization", required = false) String header) {
        Long tutorId = jwtService.extractTutorId(header);
        List<Agendamento> lista = agendamentoRepository.findByPetTutorIdOrderByDataHoraAsc(tutorId);
        lista.forEach(this::sanitize);
        return lista;
    }

    // Horarios livres em um dia para um servico (capacidade = profissionais ativos)
    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<?> buscarHorariosDisponiveis(
            @RequestParam LocalDate data,
            @RequestParam Long servicoId) {

        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (servico == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }

        boolean isVet = isVetServico(servico);
        int capacidade = profissionaisDoSetor(isVet).size();
        if (capacidade == 0) {
            return ResponseEntity.ok(new ArrayList<String>()); // ninguem para atender
        }

        LocalDateTime inicioExpediente = data.atTime(9, 0);
        LocalDateTime fimExpediente = data.atTime(17, 0);
        List<Agendamento> agendamentosDoDia =
                agendamentoRepository.findByDataHoraBetween(inicioExpediente, fimExpediente);

        List<String> horariosLivres = new ArrayList<>();
        int[] minutos = isVet ? new int[] { 0, 30 } : new int[] { 0 };

        for (int hora = 9; hora < 17; hora++) {
            for (int min : minutos) {
                LocalDateTime slot = data.atTime(hora, min);
                long ocupados = agendamentosDoDia.stream()
                        .filter(a -> a.getDataHora().equals(slot))
                        .filter(a -> isVetServico(a.getServico()) == isVet)
                        .count();
                if (ocupados < capacidade) {
                    horariosLivres.add(String.format("%02d:%02d", hora, min));
                }
            }
        }

        return ResponseEntity.ok(horariosLivres);
    }

    // Cria um agendamento, designando um profissional livre do setor
    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Agendamento agendamento) {
        LocalDateTime dataHora = agendamento.getDataHora();
        if (dataHora == null) {
            return ResponseEntity.badRequest().body("Erro: dataHora é obrigatória.");
        }

        // VALIDAÇÃO 1: Horário de funcionamento (09h às 17h)
        if (dataHora.getHour() < 9 || dataHora.getHour() >= 17) {
            return ResponseEntity.badRequest().body("Erro: O Rabitto funciona apenas das 09:00 às 17:00.");
        }

        if (agendamento.getServico() == null || agendamento.getServico().getId() == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço é obrigatório.");
        }
        Servico servicoEscolhido = servicoRepository.findById(agendamento.getServico().getId()).orElse(null);
        if (servicoEscolhido == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }
        boolean isVet = isVetServico(servicoEscolhido);

        // VALIDAÇÃO 2: Granularidade do slot
        if (isVet) {
            if (dataHora.getMinute() != 0 && dataHora.getMinute() != 30) {
                return ResponseEntity.badRequest()
                        .body("Erro: Consultas devem ser em intervalos de 30 min (ex: 14:00, 14:30).");
            }
        } else {
            if (dataHora.getMinute() != 0) {
                return ResponseEntity.badRequest().body("Erro: Banhos devem ser marcados em hora cheia (ex: 10:00).");
            }
        }

        // VALIDAÇÃO 3: Designar um profissional livre (previne sobreposição por profissional)
        List<Funcionario> profissionais = profissionaisDoSetor(isVet);
        if (profissionais.isEmpty()) {
            return ResponseEntity.badRequest().body(isVet
                    ? "Erro: Nenhum veterinário disponível na clínica."
                    : "Erro: Nenhum tosador disponível na loja.");
        }

        Set<Long> ocupadosIds = agendamentoRepository.findByDataHora(dataHora).stream()
                .filter(a -> a.getFuncionario() != null)
                .map(a -> a.getFuncionario().getId())
                .collect(Collectors.toSet());

        Funcionario livre = profissionais.stream()
                .filter(f -> !ocupadosIds.contains(f.getId()))
                .findFirst()
                .orElse(null);

        if (livre == null) {
            return ResponseEntity.badRequest().body(isVet
                    ? "Erro: O veterinário já está ocupado nesse horário."
                    : "Erro: Todos os tosadores já estão ocupados nesse horário.");
        }

        agendamento.setServico(servicoEscolhido);
        agendamento.setFuncionario(livre);
        if (agendamento.getStatus() == null || agendamento.getStatus().isBlank()) {
            agendamento.setStatus("Pendente");
        }

        Agendamento salvo = agendamentoRepository.save(agendamento);
        sanitize(salvo);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    public Agendamento atualizar(@PathVariable Long id, @RequestBody Agendamento agendamentoAtualizado) {
        agendamentoAtualizado.setId(id);
        Agendamento salvo = agendamentoRepository.save(agendamentoAtualizado);
        sanitize(salvo);
        return salvo;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String novoStatus = body.get("status");
        if (novoStatus == null || novoStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Erro: campo 'status' é obrigatório.");
        }
        if (!STATUS_PERMITIDOS.contains(novoStatus)) {
            return ResponseEntity.badRequest().body("Erro: status inválido. Use: " + STATUS_PERMITIDOS);
        }

        return agendamentoRepository.findById(id)
                .map(agendamento -> {
                    agendamento.setStatus(novoStatus);
                    Agendamento salvo = agendamentoRepository.save(agendamento);
                    sanitize(salvo);
                    return ResponseEntity.ok(salvo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        agendamentoRepository.deleteById(id);
    }

    // ---- helpers ----

    private boolean isVetServico(Servico servico) {
        if (servico == null || servico.getNome() == null) {
            return false;
        }
        String nome = servico.getNome().toLowerCase();
        return nome.contains("consulta") || nome.contains("vacina");
    }

    private List<Funcionario> profissionaisDoSetor(boolean isVet) {
        return funcionarioRepository.findByAtivoTrue().stream()
                .filter(f -> {
                    String cargo = f.getCargo() == null ? "" : f.getCargo().toUpperCase();
                    return isVet
                            ? cargo.contains("VETERIN")
                            : (cargo.contains("TOSAD") || cargo.contains("BANHIST"));
                })
                .toList();
    }

    /** Remove dados sensiveis do profissional antes de serializar. */
    private void sanitize(Agendamento a) {
        if (a != null && a.getFuncionario() != null) {
            a.getFuncionario().setSenha(null);
        }
    }
}
