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
                .findByFuncionariosIdAndDataHoraBetweenOrderByDataHoraAsc(vetId, inicio, fim)
                .stream()
                .filter(a -> a.getServicos().stream().anyMatch(this::isVetServico))
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

    // Horarios livres em um dia para um ou mais servicos.
    // Um agendamento pode misturar setores (ex: Consulta + Banho); o horario
    // so e livre se houver capacidade em TODOS os setores envolvidos.
    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<?> buscarHorariosDisponiveis(
            @RequestParam LocalDate data,
            @RequestParam("servicoId") List<Long> servicoIds) {

        if (servicoIds == null || servicoIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Informe ao menos um serviço.");
        }

        List<Servico> servicos = servicoRepository.findAllById(servicoIds);
        if (servicos.size() != new java.util.HashSet<>(servicoIds).size()) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }

        boolean precisaVet = servicos.stream().anyMatch(this::isVetServico);
        boolean precisaBanho = servicos.stream().anyMatch(s -> !isVetServico(s));

        int capVet = precisaVet ? profissionaisDoSetor(true).size() : Integer.MAX_VALUE;
        int capBanho = precisaBanho ? profissionaisDoSetor(false).size() : Integer.MAX_VALUE;
        if ((precisaVet && capVet == 0) || (precisaBanho && capBanho == 0)) {
            return ResponseEntity.ok(new ArrayList<String>()); // setor sem ninguem para atender
        }

        LocalDateTime inicioExpediente = data.atTime(9, 0);
        LocalDateTime fimExpediente = data.atTime(17, 0);
        List<Agendamento> agendamentosDoDia =
                agendamentoRepository.findByDataHoraBetween(inicioExpediente, fimExpediente);

        List<String> horariosLivres = new ArrayList<>();
        // Banho so em hora cheia; consultas aceitam 30 min. Misto => hora cheia.
        int[] minutos = precisaBanho ? new int[] { 0 } : new int[] { 0, 30 };

        for (int hora = 9; hora < 17; hora++) {
            for (int min : minutos) {
                LocalDateTime slot = data.atTime(hora, min);
                List<Agendamento> noSlot = agendamentosDoDia.stream()
                        .filter(a -> a.getDataHora().equals(slot))
                        .collect(Collectors.toList());

                long ocupVet = noSlot.stream()
                        .filter(a -> a.getServicos().stream().anyMatch(this::isVetServico))
                        .count();
                long ocupBanho = noSlot.stream()
                        .filter(a -> a.getServicos().stream().anyMatch(s -> !isVetServico(s)))
                        .count();

                boolean livreVet = !precisaVet || ocupVet < capVet;
                boolean livreBanho = !precisaBanho || ocupBanho < capBanho;
                if (livreVet && livreBanho) {
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

        // VALIDAÇÃO 2: Servicos selecionados (um ou mais)
        if (agendamento.getServicos() == null || agendamento.getServicos().isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Selecione ao menos um serviço.");
        }
        List<Long> servicoIds = agendamento.getServicos().stream()
                .map(Servico::getId)
                .distinct()
                .collect(Collectors.toList());
        if (servicoIds.contains(null)) {
            return ResponseEntity.badRequest().body("Erro: Serviço inválido.");
        }
        List<Servico> servicosEscolhidos = servicoRepository.findAllById(servicoIds);
        if (servicosEscolhidos.size() != servicoIds.size()) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado.");
        }

        boolean precisaVet = servicosEscolhidos.stream().anyMatch(this::isVetServico);
        boolean precisaBanho = servicosEscolhidos.stream().anyMatch(s -> !isVetServico(s));

        // VALIDAÇÃO 3: Granularidade do slot (banho => hora cheia; misto => hora cheia)
        if (precisaBanho) {
            if (dataHora.getMinute() != 0) {
                return ResponseEntity.badRequest().body("Erro: Banhos devem ser marcados em hora cheia (ex: 10:00).");
            }
        } else if (dataHora.getMinute() != 0 && dataHora.getMinute() != 30) {
            return ResponseEntity.badRequest()
                    .body("Erro: Consultas devem ser em intervalos de 30 min (ex: 14:00, 14:30).");
        }

        // VALIDAÇÃO 4: Designar um profissional livre por setor envolvido
        Set<Long> ocupadosIds = agendamentoRepository.findByDataHora(dataHora).stream()
                .flatMap(a -> a.getFuncionarios().stream())
                .map(Funcionario::getId)
                .collect(Collectors.toSet());

        List<Funcionario> designados = new ArrayList<>();
        if (precisaVet) {
            List<Funcionario> vets = profissionaisDoSetor(true);
            if (vets.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Nenhum veterinário disponível na clínica.");
            }
            Funcionario livre = vets.stream()
                    .filter(f -> !ocupadosIds.contains(f.getId()))
                    .findFirst()
                    .orElse(null);
            if (livre == null) {
                return ResponseEntity.badRequest().body("Erro: O veterinário já está ocupado nesse horário.");
            }
            designados.add(livre);
            ocupadosIds.add(livre.getId());
        }
        if (precisaBanho) {
            List<Funcionario> tosadores = profissionaisDoSetor(false);
            if (tosadores.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Nenhum tosador disponível na loja.");
            }
            Funcionario livre = tosadores.stream()
                    .filter(f -> !ocupadosIds.contains(f.getId()))
                    .findFirst()
                    .orElse(null);
            if (livre == null) {
                return ResponseEntity.badRequest().body("Erro: Todos os tosadores já estão ocupados nesse horário.");
            }
            designados.add(livre);
            ocupadosIds.add(livre.getId());
        }

        agendamento.setServicos(servicosEscolhidos);
        agendamento.setFuncionarios(designados);
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
        if (a != null && a.getFuncionarios() != null) {
            a.getFuncionarios().forEach(f -> {
                if (f != null) {
                    f.setSenha(null);
                }
            });
        }
    }
}
