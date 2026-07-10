package com.vacancyscout.controller;

import com.vacancyscout.model.Company;
import com.vacancyscout.service.company.CompanyService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public Flux<Company> list() {
        return companyService.listAll();
    }

    @GetMapping("/{id}")
    public Mono<Company> get(@PathVariable UUID id) {
        return companyService.getById(id);
    }

    @PostMapping
    public Mono<Company> create(@RequestBody Company company) {
        return companyService.create(company);
    }

    @PutMapping("/{id}")
    public Mono<Company> update(@PathVariable UUID id, @RequestBody Company company) {
        return companyService.update(id, company);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable UUID id) {
        return companyService.delete(id);
    }
}
