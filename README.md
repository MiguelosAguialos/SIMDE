# Frontend do Projeto

Este repositório contém a aplicação frontend do projeto, desenvolvida com React, TypeScript e Vite.

## Requisitos

- Node.js instalado
- `npm` disponível no ambiente

## Instalação

No diretório raiz do projeto, instale as dependências:

```bash
npm install --force
```

## Inicialização

Para iniciar o projeto em ambiente de desenvolvimento, execute:

```bash
npm run dev
```

O Vite irá subir a aplicação em modo desenvolvimento e exibir no terminal o endereço local para acesso no navegador.

## Scripts disponíveis

- `npm run dev`: inicia o servidor de desenvolvimento
- `npm run build`: gera a versão de produção
- `npm start`: executa a aplicação em modo produção
- `npm run preview`: visualiza a build localmente
- `npm run check`: verifica tipos do TypeScript
- `npm run format`: formata os arquivos com Prettier

## Observações

- Caso seja a primeira vez executando o projeto, rode `npm install` antes de `npm run dev`.
- Se houver erro de porta ocupada, encerre o processo que está usando a porta ou ajuste a configuração do Vite.
