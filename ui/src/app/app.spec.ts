import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let requetes: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    requetes = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requetes.verify());

  it("affiche la version renvoyée par l'API", async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    requetes.expectOne('/api/version').flush({
      application: 'Endeavor : Eaux profondes',
      version: '0.1.0-SNAPSHOT',
    });

    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('0.1.0-SNAPSHOT');
  });

  it("signale une API injoignable", async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    requetes.expectOne('/api/version').error(new ProgressEvent('erreur reseau'));

    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ne répond pas');
  });
});
