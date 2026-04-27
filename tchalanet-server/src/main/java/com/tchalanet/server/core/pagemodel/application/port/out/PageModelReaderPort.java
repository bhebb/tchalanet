package com.tchalanet.server.core.pagemodel.application.port.out;

/**
 * Alias canonique de lecture pour le domaine pagemodel.
 * Étend {@link PageModelReadPort} sans ajouter de méthodes supplémentaires.
 * Permet aux nouveaux handlers d'utiliser le nom conventionnel *ReaderPort
 * (convention typed_ids §2 / ARCHITECTURE.md) sans casser les handlers existants.
 */
public interface PageModelReaderPort extends PageModelReadPort {}

