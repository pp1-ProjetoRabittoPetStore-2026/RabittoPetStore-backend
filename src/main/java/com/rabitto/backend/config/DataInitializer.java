package com.rabitto.backend.config;

import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.FuncionarioRepository;
import com.rabitto.backend.repositories.ServicoRepository;
import com.rabitto.backend.security.Roles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(FuncionarioRepository funcionarioRepository,
                           ServicoRepository servicoRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (funcionarioRepository.count() == 0) {
                funcionarioRepository.save(staff("Gerente Rabitto", Roles.GERENTE,
                        "00000000000", "gerente@rabitto.com", passwordEncoder.encode("gerente123")));
                funcionarioRepository.save(staff("Dra. Marina Vet", Roles.VETERINARIO,
                        "11111111111", "vet@rabitto.com", passwordEncoder.encode("vet123")));
                funcionarioRepository.save(staff("Carlos Tosador", Roles.TOSADOR,
                        "22222222222", "tosador@rabitto.com", passwordEncoder.encode("tosador123")));
            }

            if (servicoRepository.count() == 0) {
                servicoRepository.save(servico("Banho", "Banho completo", 60.0));
                servicoRepository.save(servico("Tosa", "Tosa higiênica", 80.0));
                servicoRepository.save(servico("Consulta Veterinária", "Consulta clínica", 150.0));
                servicoRepository.save(servico("Vacina", "Aplicação de vacina", 90.0));
            }
        };
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

    private Servico servico(String nome, String descricao, Double preco) {
        Servico s = new Servico();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setPreco(preco);
        return s;
    }
}
