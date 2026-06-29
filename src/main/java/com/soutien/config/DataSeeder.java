package com.soutien.config;

import com.soutien.entity.Role;
import com.soutien.entity.Subject;
import com.soutien.entity.User;
import com.soutien.repository.SubjectRepository;
import com.soutien.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cree quelques comptes et matieres de demonstration au demarrage,
 * uniquement si la base est vide (utile avec H2 en memoire).
 * Mots de passe en clair (pour les tests manuels) : "password123" pour tous.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String pwd = passwordEncoder.encode("password123");

        userRepository.save(User.builder().nom("Amine Admin").email("admin@soutien.ma").password(pwd).role(Role.ADMIN).build());
        userRepository.save(User.builder().nom("Yassine Eleve").email("eleve@soutien.ma").password(pwd).role(Role.ELEVE).build());
        userRepository.save(User.builder().nom("Sara Enseignante").email("enseignant@soutien.ma").password(pwd).role(Role.ENSEIGNANT).build());

        subjectRepository.save(Subject.builder().nom("Mathematiques").description("Algebre, analyse, geometrie").build());
        subjectRepository.save(Subject.builder().nom("Physique").description("Mecanique, electricite").build());
        subjectRepository.save(Subject.builder().nom("Informatique").description("Algorithmique, programmation").build());
    }
}
