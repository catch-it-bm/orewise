import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  authInterceptor,
  provideAuth,
  StsConfigHttpLoader,
  StsConfigLoader,
  withAppInitializerAuthCheck,
} from 'angular-auth-oidc-client';
import { map } from 'rxjs';
import { routes } from './app.routes';

/** Served as /config.json so self-hosted installs can change it without a rebuild. */
interface RuntimeConfig {
  oidcAuthority: string;
  oidcClientId: string;
}

const authConfigLoader = (http: HttpClient) =>
  new StsConfigHttpLoader(
    http.get<RuntimeConfig>('config.json').pipe(
      map((c) => ({
        authority: c.oidcAuthority,
        clientId: c.oidcClientId,
        redirectUrl: window.location.origin,
        postLogoutRedirectUri: window.location.origin,
        scope: 'openid profile email',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        secureRoutes: ['/api/'],
      })),
    ),
  );

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor()])),
    provideAuth(
      { loader: { provide: StsConfigLoader, useFactory: authConfigLoader, deps: [HttpClient] } },
      withAppInitializerAuthCheck(),
    ),
  ],
};
