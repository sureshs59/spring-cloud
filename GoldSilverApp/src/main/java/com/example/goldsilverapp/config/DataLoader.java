package com.example.goldsilverapp.config;

import com.example.goldsilverapp.entity.MetalRateEntity;
import com.example.goldsilverapp.repository.MetalRateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {

    private final MetalRateRepository repository;

    public DataLoader(MetalRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {

        if (repository.count() > 0) {
            return;
        }

        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(6), 70500, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(5), 71000, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(4), 71250, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(3), 71800, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(2), 72000, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now().minusDays(1), 71800, "10 gram"));
        repository.save(new MetalRateEntity("Gold", LocalDate.now(), 72500, "10 gram"));

        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(6), 82500, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(5), 83000, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(4), 83800, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(3), 84500, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(2), 84000, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now().minusDays(1), 84200, "1 kg"));
        repository.save(new MetalRateEntity("Silver", LocalDate.now(), 83500, "1 kg"));
    }
}