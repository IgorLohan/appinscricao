package com.aula.pos.appinscricao.service.impl;

import com.aula.pos.appinscricao.dto.EventoRequest;
import com.aula.pos.appinscricao.dto.EventoResponse;
import com.aula.pos.appinscricao.exception.EventoDuplicadoException;
import com.aula.pos.appinscricao.exception.EventoJaRealizadoException;
import com.aula.pos.appinscricao.exception.ResourceNotFoundException;
import com.aula.pos.appinscricao.mapper.EventoMapper;
import com.aula.pos.appinscricao.model.Evento;
import com.aula.pos.appinscricao.repository.EventoRepository;
import com.aula.pos.appinscricao.service.EventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final EventoMapper mapper;

    @Override
    @Transactional
    public EventoResponse save(EventoRequest request) {
        validarDuplicidade(request, null);
        var evento = mapper.toEntity(request);
        return mapper.toResponse(eventoRepository.save(evento));
    }

    @Override
    public EventoResponse findById(UUID id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Override
    public Page<EventoResponse> findAll(String nome, String cidade, String estado, Pageable pageable) {
        Page<Evento> page;

        if (nome != null && !nome.isBlank()) {
            page = eventoRepository.findByNomeContainingIgnoreCase(nome, pageable);
        } else if (cidade != null && !cidade.isBlank()) {
            page = eventoRepository.findByCidadeIgnoreCase(cidade, pageable);
        } else if (estado != null && !estado.isBlank()) {
            page = eventoRepository.findByEstadoIgnoreCase(estado, pageable);
        } else {
            page = eventoRepository.findAll(pageable);
        }

        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public EventoResponse update(UUID id, EventoRequest request) {
        var evento = findByIdOrThrow(id);
        validarDuplicidade(request, id);
        mapper.updateEntity(request, evento);
        return mapper.toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        var evento = findByIdOrThrow(id);
        if (evento.getDataHoraEvento().isBefore(LocalDateTime.now())) {
            throw new EventoJaRealizadoException();
        }
        eventoRepository.delete(evento);
    }

    private Evento findByIdOrThrow(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private void validarDuplicidade(EventoRequest request, UUID idIgnorado) {
        boolean duplicado = idIgnorado == null
                ? eventoRepository.existsByNomeIgnoreCaseAndDataHoraEventoAndCidadeIgnoreCase(
                        request.nome(), request.dataHoraEvento(), request.cidade())
                : eventoRepository.existsByNomeIgnoreCaseAndDataHoraEventoAndCidadeIgnoreCaseAndIdNot(
                        request.nome(), request.dataHoraEvento(), request.cidade(), idIgnorado);

        if (duplicado) {
            throw new EventoDuplicadoException();
        }
    }
}
