import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api, Me, Plan, PLANS, Tenant } from '../api';

const GIB = 1024 ** 3;

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly api = inject(Api);

  protected readonly plans = PLANS;
  protected readonly me = signal<Me | null>(null);
  protected readonly tenants = signal<Tenant[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9-]{1,62}$/)]],
    name: ['', Validators.required],
    plan: ['BASIC' as Plan, Validators.required],
  });

  ngOnInit(): void {
    this.api.me().subscribe((me) => {
      this.me.set(me);
      if (me.roles.includes('platform-admin')) {
        this.loadTenants();
      }
    });
  }

  protected gb(bytes: number): number {
    return bytes / GIB;
  }

  protected createTenant(): void {
    this.api.createTenant(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset();
        this.error.set(null);
        this.loadTenants();
      },
      error: (e) => this.error.set(e.status === 409 ? 'Slug already exists' : 'Could not create tenant'),
    });
  }

  protected changePlan(slug: string, plan: string): void {
    this.api.changePlan(slug, plan as Plan).subscribe(() => this.loadTenants());
  }

  private loadTenants(): void {
    this.api.tenants().subscribe((t) => this.tenants.set(t));
  }
}
