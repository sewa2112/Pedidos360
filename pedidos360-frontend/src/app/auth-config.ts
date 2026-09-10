import {
  BrowserCacheLocation,
  Configuration,
  LogLevel
} from '@azure/msal-browser';

export const tenantId = 'ac6019c7-78df-4d97-9d85-291001476eb0';

export const frontendClientId = '4ad65278-8f7b-4a8f-844c-6dd0ad3736a2';

export const authority = `https://login.microsoftonline.com/${tenantId}`;

export const redirectUri = 'http://localhost:4200';

export const apiScope = 'api://cfd3c072-73f2-4aba-b0b3-e2450e8d4117/access_as_user';

export const msalConfig: Configuration = {
  auth: {
    clientId: frontendClientId,
    authority: authority,
    redirectUri: redirectUri,
    postLogoutRedirectUri: redirectUri
  },
  cache: {
    cacheLocation: BrowserCacheLocation.LocalStorage
  },
  system: {
    allowPlatformBroker: false,
    loggerOptions: {
      loggerCallback: (
        logLevel: LogLevel,
        message: string,
        containsPii: boolean
      ): void => {
        if (containsPii) {
          return;
        }
        console.log(`[MSAL ${LogLevel[logLevel]}] ${message}`);
      },
      logLevel: LogLevel.Info,
      piiLoggingEnabled: false
    }
  }
};

export const loginRequest = {
  scopes: [
    'openid',
    'profile',
    apiScope
  ]
};