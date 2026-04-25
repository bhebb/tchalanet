package com.tchalanet.server.core.pagemodel.application.port.out;

/**
 * Alias canonique d'écriture pour le domaine pagemodel.
 * Étend {@link PageModelWritePort} sans ajouter de méthodes supplémentaires.
 * Permet aux nouveaux handlers d'utiliser le nom conventionnel *WriterPort
 * (convention typed_ids §2 / ARCHITECTURE.md) sans casser les handlers existants.
 */
public interface PageModelWriterPort extends PageModelWritePort {}

