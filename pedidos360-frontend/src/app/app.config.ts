import {
 ApplicationConfig,
 provideBrowserGlobalErrorListeners

} from '@angular/core';
import { provideRouter } from '@angular/router';
import {
 IPublicClientApplication,
 PublicClientApplication
} from '@azure/msal-browser';
import {
 MSAL_INSTANCE,
 MsalBroadcastService,
 MsalService
} from '@azure/msal-angular';
import { routes } from './app.routes';
import { msalConfig } from './auth-config';

//HttpClient
import { provideHttpClient } from '@angular/common/http';

export function msalInstanceFactory(): IPublicClientApplication {
 return new PublicClientApplication(msalConfig);
}
export const appConfig: ApplicationConfig = {
 providers: [
 provideBrowserGlobalErrorListeners(),
 provideRouter(routes),
 // 2. AGREGAR HttpClient A LOS PROVIDERS
    provideHttpClient(),
 {
 provide: MSAL_INSTANCE,
 useFactory: msalInstanceFactory
 },
 MsalService,
 MsalBroadcastService
 ]
};