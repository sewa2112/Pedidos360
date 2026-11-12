import { InteractionType } from '@azure/msal-browser';
import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  const protectedResourceMap = new Map<string, Array<string>>([
    ['https://0i74gnrx68.execute-api.us-east-1.amazonaws.com/*', environment.azure.protectedResourceScopes],
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes],
    ['/api/*', environment.azure.protectedResourceScopes],
    [`${origin}/api/*`, environment.azure.protectedResourceScopes],
    ['http://localhost:8085/*', environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
