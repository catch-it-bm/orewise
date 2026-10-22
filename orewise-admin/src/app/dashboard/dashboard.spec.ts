import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows the tenant plan and limits for a tenant user', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    http.expectOne('/api/me').flush({
      username: 'jan',
      roles: ['tenant-admin'],
      tenant: { slug: 'demo', name: 'Demo BV', plan: 'BASIC', docLimit: 1000, storageLimitBytes: 2 * 1024 ** 3 },
    });
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent!;
    expect(text).toContain('Demo BV');
    expect(text).toContain('BASIC');
    expect(text).toContain('up to 1000');
    expect(text).toContain('up to 2 GB');
    expect(text).not.toContain('Tenants');
  });

  it('lets a platform admin list and create tenants', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    http.expectOne('/api/me').flush({ username: 'root', roles: ['platform-admin'], tenant: null });
    http.expectOne('/api/admin/tenants').flush([
      { slug: 'demo', name: 'Demo BV', plan: 'BASIC', docLimit: 1000, storageLimitBytes: 2 * 1024 ** 3 },
    ]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Demo BV');

    const component = fixture.componentInstance as unknown as {
      form: { setValue(v: object): void };
      createTenant(): void;
    };
    component.form.setValue({ slug: 'acme', name: 'Acme NV', plan: 'PRO' });
    component.createTenant();

    const create = http.expectOne({ method: 'POST', url: '/api/admin/tenants' });
    expect(create.request.body).toEqual({ slug: 'acme', name: 'Acme NV', plan: 'PRO' });
    create.flush({});
    http.expectOne('/api/admin/tenants').flush([]);
  });
});
