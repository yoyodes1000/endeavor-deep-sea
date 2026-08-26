import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/** Réponse de GET /api/version. */
interface VersionApi {
  application: string;
  version: string;
}

/**
 * Page d'accueil du squelette.
 *
 * Elle n'a qu'un rôle : prouver que l'interface parvient à joindre l'API.
 * Elle sera remplacée dès que le moteur saura décrire une partie.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);

  protected readonly version = signal<VersionApi | null>(null);
  protected readonly erreur = signal<string | null>(null);

  constructor() {
    this.http.get<VersionApi>('/api/version').subscribe({
      next: (reponse) => this.version.set(reponse),
      error: () => this.erreur.set("L'API ne répond pas."),
    });
  }
}
