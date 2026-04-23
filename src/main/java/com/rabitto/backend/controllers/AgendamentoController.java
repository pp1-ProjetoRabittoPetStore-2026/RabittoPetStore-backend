package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    // chama o repositório de serviços para descobrir o nome do serviço escolhido
    @Autowired
    private ServicoRepository servicoRepository;

    @GetMapping
    public List<Agendamento> listarTodos() {
        return agendamentoRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Agendamento agendamento) {
        LocalDateTime dataHora = agendamento.getDataHora();

        // REGRA 1: Horário de funcionamento (09h00 às 17h00)
        if (dataHora.getHour() < 9 || dataHora.getHour() >= 17) {
            return ResponseEntity.badRequest().body("Erro: O horário de funcionamento do Rabitto é das 09:00 às 17:00.");
        }

        // Busca o serviço completo no banco para saber do que se trata
        Servico servicoEscolhido = servicoRepository.findById(agendamento.getServico().getId()).orElse(null);
        if (servicoEscolhido == null) {
            return ResponseEntity.badRequest().body("Erro: Serviço não encontrado no banco de dados.");
        }

        // Identifica se é serviço de veterinário pela palavra no nome
        String nomeServico = servicoEscolhido.getNome().toLowerCase();
        boolean isVeterinario = nomeServico.contains("consulta") || nomeServico.contains("vacina");

        // Busca todos os agendamentos que já estão marcados para essa mesma data e hora exatas
        List<Agendamento> agendamentosNoHorario = agendamentoRepository.findByDataHora(dataHora);

        int contagemVet = 0;
        int contagemBanho = 0;

        // Conta quantos de cada tipo já existem naquele horário
        for (Agendamento agendado : agendamentosNoHorario) {
            // Puxa o serviço do agendamento que já estava salvo no banco para checar o nome
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

        // REGRA 2: Validações de limites e duração
        if (isVeterinario) {
            // Só existe 1 veterinário
            if (contagemVet >= 1) {
                return ResponseEntity.badRequest().body("Erro: O veterinário já possui uma consulta marcada para este horário.");
            }
            // Consultas duram 30 min (só aceita agendar em hora cheia :00 ou meia hora :30)
            if (dataHora.getMinute() != 0 && dataHora.getMinute() != 30) {
                return ResponseEntity.badRequest().body("Erro: Consultas veterinárias devem ser marcadas em intervalos de 30 minutos (ex: 09:00, 09:30).");
            }
        } else {
            // Só existem 2 banhistas
            if (contagemBanho >= 2) {
                return ResponseEntity.badRequest().body("Erro: Todos os banhistas já estão ocupados neste horário.");
            }
            // Banhos duram 1 hora (só aceita agendar em hora cheia :00)
            if (dataHora.getMinute() != 0) {
                return ResponseEntity.badRequest().body("Erro: Banhos e tosas devem ser marcados em hora cheia (ex: 09:00, 10:00).");
            }
        }

        // Se passou por todas as barreiras, o código finalmente salva no banco!
        Agendamento salvo = agendamentoRepository.save(agendamento);
        return ResponseEntity.ok(salvo);
    }

    // ... (Os métodos de PUT e DELETE continuam iguais, tu podes colar eles aqui embaixo) ...
}