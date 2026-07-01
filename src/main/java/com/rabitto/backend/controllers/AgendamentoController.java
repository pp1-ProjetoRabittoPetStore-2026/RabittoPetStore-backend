package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.FuncionarioRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import com.rabitto.backend.services.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

    private static final Logger log = LoggerFactory.getLogger(AgendamentoController.class);

    private static final List<String> STATUS_PERMITIDOS =
            List.of("Pendente", "Aguardando", "Em Serviço", "Pronto", "Rejeitado", "Cancelado", "Confirmado");

    

    private static final Set<String> STATUS_LIBERADOS = Set.of("Rejeitado", "Cancelado");

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private JwtService jwtService;

    

    @GetMapping(value = { "/status", "/status/{status}" })
    public List<Agendamento> listar(@PathVariable(required = false) String status) {
        List<Agendamento> lista = (status != null && !status.isEmpty())
                ? agendamentoRepository.findByStatusOrderByDataHoraAsc(status)
                : agendamentoRepository.findAllByOrderByDataHoraAsc();
        lista.forEach(this::sanitize);
        return lista;
    }

    

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

    

    @GetMapping("/meus")
    public List<Agendamento> meusAgendamentos(
            @RequestHeader(value = "Authorization", required = false) String header) {
        Long tutorId = jwtService.extractTutorId(header);
        List<Agendamento> lista = agendamentoRepository.findByPetTutorIdOrderByDataHoraAsc(tutorId);
        lista.forEach(this::sanitize);
        return lista;
    }

    

    

    

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
        boolean semCapacidade = (precisaVet && capVet == 0) || (precisaBanho && capBanho == 0);

        LocalDateTime inicioExpediente = data.atTime(9, 0);
        LocalDateTime fimExpediente = data.atTime(17, 0);
        List<Agendamento> agendamentosDoDia = semCapacidade
                ? new ArrayList<>()
                : agendamentoRepository.findByDataHoraBetween(inicioExpediente, fimExpediente);

        List<HorarioSlot> horarios = new ArrayList<>();


        int[] minutos = precisaBanho ? new int[] { 0 } : new int[] { 0, 30 };

        for (int hora = 9; hora < 17; hora++) {
            for (int min : minutos) {
                String label = String.format("%02d:%02d", hora, min);
                if (semCapacidade) {
                    horarios.add(new HorarioSlot(label, false));
                    continue;
                }

                LocalDateTime slot = data.atTime(hora, min);
                List<Agendamento> noSlot = agendamentosDoDia.stream()
                        .filter(a -> a.getDataHora().equals(slot))
                        .filter(this::ocupaCapacidade)
                        .collect(Collectors.toList());

                long ocupVet = noSlot.stream()
                        .filter(a -> a.getServicos().stream().anyMatch(this::isVetServico))
                        .count();
                long ocupBanho = noSlot.stream()
                        .filter(a -> a.getServicos().stream().anyMatch(s -> !isVetServico(s)))
                        .count();

                boolean livreVet = !precisaVet || ocupVet < capVet;
                boolean livreBanho = !precisaBanho || ocupBanho < capBanho;
                horarios.add(new HorarioSlot(label, livreVet && livreBanho));
            }
        }

        return ResponseEntity.ok(horarios);
    }


    public record HorarioSlot(String hora, boolean disponivel) {
    }

    

    @PostMapping
    @Transactional
    public ResponseEntity<?> salvar(@RequestBody Agendamento agendamento) {
        LocalDateTime dataHora = agendamento.getDataHora();
        if (dataHora == null) {
            return ResponseEntity.badRequest().body("Erro: dataHora é obrigatória.");
        }

        

        if (dataHora.getHour() < 9 || dataHora.getHour() >= 17) {
            return ResponseEntity.badRequest().body("Erro: O Rabitto funciona apenas das 09:00 às 17:00.");
        }

        

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



        Long petId = agendamento.getPet() != null ? agendamento.getPet().getId() : null;
        if (petId == null) {
            return ResponseEntity.badRequest().body("Erro: Pet é obrigatório.");
        }
        boolean duplicado = agendamentoRepository
                .findByPetIdAndDataHoraAndServicosIdIn(petId, dataHora, servicoIds)
                .stream()
                .anyMatch(this::ocupaCapacidade);
        if (duplicado) {
            log.warn("Agendamento rejeitado (duplicado): petId={} dataHora={} servicoIds={}", petId, dataHora, servicoIds);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Erro: Este pet já possui um agendamento para um destes serviços neste horário.");
        }

        boolean precisaVet = servicosEscolhidos.stream().anyMatch(this::isVetServico);
        boolean precisaBanho = servicosEscolhidos.stream().anyMatch(s -> !isVetServico(s));



        if (precisaBanho) {
            if (dataHora.getMinute() != 0) {
                return ResponseEntity.badRequest().body("Erro: Banhos devem ser marcados em hora cheia (ex: 10:00).");
            }
        } else if (dataHora.getMinute() != 0 && dataHora.getMinute() != 30) {
            return ResponseEntity.badRequest()
                    .body("Erro: Consultas devem ser em intervalos de 30 min (ex: 14:00, 14:30).");
        }

        

        Set<Long> ocupadosIds = agendamentoRepository.findByDataHora(dataHora).stream()
                .filter(this::ocupaCapacidade)
                .flatMap(a -> a.getFuncionarios().stream())
                .map(Funcionario::getId)
                .collect(Collectors.toSet());

        List<Funcionario> designados = new ArrayList<>();
        if (precisaVet) {
            List<Funcionario> vets = profissionaisDoSetor(true);
            if (vets.isEmpty()) {
                log.warn("Agendamento rejeitado: nenhum veterinario cadastrado/ativo");
                return ResponseEntity.badRequest().body("Erro: Nenhum veterinário disponível na clínica.");
            }
            Funcionario livre = vets.stream()
                    .filter(f -> !ocupadosIds.contains(f.getId()))
                    .findFirst()
                    .orElse(null);
            if (livre == null) {
                log.warn("Agendamento rejeitado: todos os {} veterinarios ocupados em dataHora={}", vets.size(), dataHora);
                return ResponseEntity.badRequest().body("Erro: O veterinário já está ocupado nesse horário.");
            }
            designados.add(livre);
            ocupadosIds.add(livre.getId());
        }
        if (precisaBanho) {
            List<Funcionario> tosadores = profissionaisDoSetor(false);
            if (tosadores.isEmpty()) {
                log.warn("Agendamento rejeitado: nenhum tosador cadastrado/ativo");
                return ResponseEntity.badRequest().body("Erro: Nenhum tosador disponível na loja.");
            }
            Funcionario livre = tosadores.stream()
                    .filter(f -> !ocupadosIds.contains(f.getId()))
                    .findFirst()
                    .orElse(null);
            if (livre == null) {
                log.warn("Agendamento rejeitado: todos os {} tosadores ocupados em dataHora={}", tosadores.size(), dataHora);
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
        log.info("Agendamento criado: id={} petId={} dataHora={} servicoIds={} funcionarios={}",
                salvo.getId(), petId, dataHora, servicoIds,
                designados.stream().map(Funcionario::getId).toList());
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    public Agendamento atualizar(@PathVariable Long id, @RequestBody Agendamento agendamentoAtualizado) {
        agendamentoAtualizado.setId(id);
        Agendamento salvo = agendamentoRepository.save(agendamentoAtualizado);
        sanitize(salvo);
        log.info("Agendamento atualizado: id={}", id);
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
                    String statusAnterior = agendamento.getStatus();
                    agendamento.setStatus(novoStatus);
                    Agendamento salvo = agendamentoRepository.save(agendamento);
                    sanitize(salvo);
                    log.info("Status do agendamento alterado: id={} {} -> {}", id, statusAnterior, novoStatus);
                    return ResponseEntity.ok(salvo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        agendamentoRepository.deleteById(id);
        log.info("Agendamento removido: id={}", id);
    }

    


    
    private boolean ocupaCapacidade(Agendamento a) {
        return a != null && (a.getStatus() == null || !STATUS_LIBERADOS.contains(a.getStatus()));
    }

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
