# ShopSphere Frontend

Modern, commercial e-commerce web application built with **React**, **TypeScript**, and **Vite**.

## Architecture Overview

```
frontend/
├── public/
├── src/
│   ├── assets/
│   ├── components/
│   │   ├── common/
│   │   ├── layout/         # Header, Footer, MainLayout
│   │   └── product/        # ProductCard
│   ├── pages/              # HomePage, ProductsPage, ProductDetailPage, CartPage, etc.
│   ├── routes/             # AppRoutes (React Router v7)
│   ├── services/           # Centralized Axios API Client
│   ├── types/              # TypeScript models matching Spring Boot backend DTOs
│   ├── styles/             # Design system CSS variables, layout, components
│   ├── App.tsx             # Root App component with BrowserRouter
│   └── main.tsx            # Entrypoint
├── .env.example
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Environment Configuration

Copy `.env.example` to `.env` during local development:

```env
VITE_API_BASE_URL=http://localhost:8081
```

*Note: No secrets or credentials are kept in frontend source code or environment files.*

## Available Scripts

- `npm run dev`: Start local development server on Vite
- `npm run build`: Type-check with `tsc` and produce optimized production bundle in `dist/`
- `npm run lint`: Run code linter
- `npm run preview`: Preview production build locally
