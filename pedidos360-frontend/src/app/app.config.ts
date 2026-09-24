import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners
} from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi
} from '@angular/common/http';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService
} from '@azure/msal-angular';

import { routes } from './app.routes';
import { msalInstanceFactory } from './msal-config';
import { msalInterceptorConfigFactory } from './msal-interceptor-config';
import { msalGuardConfigFactory } from './msal-guard-config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: MSAL_INSTANCE,
      useFactory: msalInstanceFactory
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: msalInterceptorConfigFactory
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: msalGuardConfigFactory
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },
    MsalGuard,
    MsalService,
    MsalBroadcastService
  ]
};