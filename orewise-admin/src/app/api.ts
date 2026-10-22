import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export type Plan = 'BASIC' | 'PRO' | 'BUSINESS' | 'ENTERPRISE';
export const PLANS: Plan[] = ['BASIC', 'PRO', 'BUSINESS', 'ENTERPRISE'];

export interface Tenant {
  slug: string;
  name: string;
  plan: Plan;
  docLimit: number;
  storageLimitBytes: number;
}

export interface Me {
  username: string;
  roles: string[];
  tenant: Tenant | null;
}

export interface CreateTenant {
  slug: string;
  name: string;
  plan: Plan;
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  me() {
    return this.http.get<Me>('/api/me');
  }

  tenants() {
    return this.http.get<Tenant[]>('/api/admin/tenants');
  }

  createTenant(req: CreateTenant) {
    return this.http.post<Tenant>('/api/admin/tenants', req);
  }

  changePlan(slug: string, plan: Plan) {
    return this.http.put<Tenant>(`/api/admin/tenants/${slug}/plan`, { plan });
  }
}
