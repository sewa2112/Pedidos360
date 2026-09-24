// src/environments/environment.ts
export const environment = {
  production: false,
  azure: {
    clientId: '4ad65278-8f7b-4a8f-844c-6dd0ad3736a2',
    tenantId: 'ac6019c7-78df-4d97-9d85-291001476eb0',
    authority: 'https://login.microsoftonline.com/ac6019c7-78df-4d97-9d85-291001476eb0',
    redirectUri: 'http://localhost:4200',
    protectedResourceScopes: ['api://f900a1e2-036a-485f-b14f-c22a1ccf7005/OT.Create']
  },
  apiBaseUrl: 'http://localhost:8085'
};

