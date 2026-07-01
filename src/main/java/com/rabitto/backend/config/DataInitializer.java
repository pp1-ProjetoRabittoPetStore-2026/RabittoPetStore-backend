package com.rabitto.backend.config;

import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.FuncionarioRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import com.rabitto.backend.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;


@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Map<String, Integer> DURACOES_PADRAO = Map.of(
            "Banho", 60,
            "Tosa", 60,
            "Consulta Veterinária", 30,
            "Vacina", 15
    );

    @Bean
    CommandLineRunner seed(FuncionarioRepository funcionarioRepository,
                           ServicoRepository servicoRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (funcionarioRepository.count() == 0) {
                log.info("Nenhum funcionario encontrado, criando contas seed");
                funcionarioRepository.save(staff("Gerente Rabitto", Roles.GERENTE,
                        "00000000000", "gerente@rabitto.com", passwordEncoder.encode("gerente123")));
                funcionarioRepository.save(staff("Dra. Marina Vet", Roles.VETERINARIO,
                        "11111111111", "vet@rabitto.com", passwordEncoder.encode("vet123")));
                funcionarioRepository.save(staff("Carlos Tosador", Roles.TOSADOR,
                        "22222222222", "tosador@rabitto.com", passwordEncoder.encode("tosador123")));
                log.info("Contas seed criadas: gerente, veterinario, tosador");
            }

            if (servicoRepository.count() == 0) {
                log.info("Nenhum servico encontrado, criando servicos seed");
                servicoRepository.save(servico("Banho", "Banho completo", 60.0, 60));
                servicoRepository.save(servico("Tosa", "Tosa higiênica", 80.0, 60));
                servicoRepository.save(servico("Consulta Veterinária", "Consulta clínica", 150.0, 30));
                servicoRepository.save(servico("Vacina", "Aplicação de vacina", 90.0, 15));
                log.info("Servicos seed criados: banho, tosa, consulta, vacina");
            }

            backfillDuracaoServicos(servicoRepository);
        };
    }

    private void backfillDuracaoServicos(ServicoRepository servicoRepository) {
        List<Servico> semDuracao = servicoRepository.findAll().stream()
                .filter(s -> s.getDuracaoMinutos() == null)
                .toList();
        if (semDuracao.isEmpty()) {
            return;
        }

        int corrigidos = 0;
        for (Servico s : semDuracao) {
            Integer duracao = DURACOES_PADRAO.get(s.getNome());
            if (duracao == null) {
                log.warn("Servico sem duracao configurada e sem default conhecido: id={} nome={}", s.getId(), s.getNome());
                continue;
            }
            s.setDuracaoMinutos(duracao);
            servicoRepository.save(s);
            corrigidos++;
        }
        log.info("Backfill de duracaoMinutos: {} servico(s) corrigido(s) de {} sem duracao", corrigidos, semDuracao.size());
    }

    private Funcionario staff(String nome, String cargo, String cpf, String email, String senhaHash) {
        Funcionario f = new Funcionario();
        f.setNome(nome);
        f.setCargo(cargo);
        f.setCpf(cpf);
        f.setEmail(email);
        f.setSenha(senhaHash);
        f.setTelefone("(81) 90000-0000");
        f.setAtivo(true);
        return f;
    }

    private Servico servico(String nome, String descricao, Double preco, Integer duracaoMinutos) {
        Servico s = new Servico();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setPreco(preco);
        s.setDuracaoMinutos(duracaoMinutos);
        return s;
    }
}
