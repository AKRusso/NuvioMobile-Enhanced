<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="NuvioMobile Enhanced" width="320" />

  <h1>NuvioMobile Enhanced</h1>

  <p><strong>Uma continuação independente do NuvioMobile, mantida por AKRusso.</strong></p>

  <p>
    Este fork mantém o Nuvio atualizado e acrescenta melhorias de experiência,
    reprodução, navegação, tracking e suporte à comunidade.
  </p>

  <p>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/releases/latest"><img src="https://img.shields.io/github/v/release/AKRusso/NuvioMobile-Enhanced?style=for-the-badge&label=Latest%20Release" alt="Latest release" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/releases"><img src="https://img.shields.io/github/downloads/AKRusso/NuvioMobile-Enhanced/total?style=for-the-badge&label=Downloads" alt="Downloads" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/actions/workflows/android-release.yml"><img src="https://img.shields.io/github/actions/workflow/status/AKRusso/NuvioMobile-Enhanced/android-release.yml?style=for-the-badge&label=Android%20Build" alt="Android build status" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/blob/enhanced/LICENSE"><img src="https://img.shields.io/github/license/AKRusso/NuvioMobile-Enhanced?style=for-the-badge" alt="License" /></a>
  </p>

  <p>
    <a href="#download">Download</a> |
    <a href="#o-que-estou-a-manter">O que estou a manter</a> |
    <a href="#funcionalidades">Funcionalidades</a> |
    <a href="#contribuir">Contribuir</a> |
    <a href="#créditos-e-atribuição">Créditos</a>
  </p>

</div>

## Estado Atual

Este repositório é mantido por **AKRusso** como uma continuação independente do
NuvioMobile Enhanced. O objetivo é acompanhar o projeto original, corrigir bugs,
melhorar a experiência Android e disponibilizar builds fáceis de instalar.

| Linha | Versão |
| --- | --- |
| Nuvio Enhanced | `0.4.5 (109)` |
| Base oficial NuvioMobile | `0.4.4` |
| Branch principal do fork | [`enhanced`](https://github.com/AKRusso/NuvioMobile-Enhanced/tree/enhanced) |
| Maintainer | [AKRusso](https://github.com/AKRusso) |

O número da versão Enhanced é separado da versão oficial para deixar claro o que
vem do Nuvio original e o que é trabalho deste fork.

## Download

### Android

Descarrega sempre a partir da release oficial do fork:

**[Download Nuvio Enhanced 0.4.5](https://github.com/AKRusso/NuvioMobile-Enhanced/releases/tag/0.4.5)**

Para a maioria dos telemóveis Android, escolhe:

**[ARM64-v8a - recomendado](https://github.com/AKRusso/NuvioMobile-Enhanced/releases/download/0.4.5/androidApp-full-arm64-v8a-release.apk)**

Outras arquiteturas estão disponíveis na página da release:

- `armeabi-v7a`: dispositivos Android antigos de 32 bits.
- `x86_64`: emuladores Intel de 64 bits.
- `x86`: emuladores Intel de 32 bits.

Os APKs de release são compilados pelo GitHub Actions, têm o certificado de
atualização compatível com as versões Enhanced anteriores e são acompanhados por
hashes SHA-256 na release.

## O Que Estou A Manter

- Atualização do fork com as versões estáveis do NuvioMobile original.
- Correções de bugs e regressões encontradas no Android.
- Builds Android assinadas e publicadas através do GitHub Actions.
- Melhorias de navegação, playback, biblioteca, Live TV e tracking.
- Documentação, changelog e releases identificáveis.
- Integração comunitária de apoiantes através do Ko-fi, sem guardar dados privados
  de pagamento.

## Funcionalidades

| Área | Melhorias do Enhanced |
| --- | --- |
| Playback | Reprodução Android com libmpv, tap-to-seek, sincronização de progresso e melhorias de estabilidade. |
| Live TV | Navegação M3U, favoritos, troca de canais, filtros, XMLTV EPG e canais recentes. |
| Tracking | Trakt e Simkl com fluxo de autenticação e sincronização atualizados. |
| Biblioteca | Calendário de lançamentos, estados mais claros e navegação refinada. |
| Assistente AI | Integrações Gemini, OpenRouter, Cerebras e Groq, com respostas formatadas. |
| Comunidade | Supporters, contribuintes, doações Ko-fi e avatares aprovados. |
| UX | Mais consistência visual, transições suaves e menos flicker em ecrãs dinâmicos. |

## Roadmap

- Continuar a acompanhar as versões oficiais do NuvioMobile.
- Corrigir problemas reportados pela comunidade e melhorar a compatibilidade Android.
- Manter releases assinadas, verificáveis e fáceis de instalar.
- Melhorar a documentação técnica e os fluxos de contribuição.

Funcionalidades podem mudar conforme a evolução do upstream e o feedback da
comunidade. Alterações específicas ficam registadas no
[`CHANGELOG.md`](CHANGELOG.md) e nas notas de cada release.

## Suporte E Feedback

- [Reportar um bug](https://github.com/AKRusso/NuvioMobile-Enhanced/issues/new/choose)
- [Ver issues abertas](https://github.com/AKRusso/NuvioMobile-Enhanced/issues)
- [Ver ações e builds](https://github.com/AKRusso/NuvioMobile-Enhanced/actions)
- [Apoiar o desenvolvimento no Ko-fi](https://ko-fi.com/nuvioenhanced)
- [Discord da comunidade](https://discord.gg/at8xffxuRU)

Ao reportar um problema, indica a versão Enhanced, arquitetura do dispositivo,
versão Android, passos para reproduzir e logs relevantes sem dados pessoais.

## Build From Source

```bash
git clone https://github.com/AKRusso/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
./gradlew :androidApp:assembleFullDebug
```

No Windows:

```powershell
git clone https://github.com/AKRusso/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
.\gradlew.bat :androidApp:assembleFullDebug
```

Para validar o projeto:

```powershell
.\gradlew.bat allTests :androidApp:lintFullDebug
```

Credenciais, tokens e configurações privadas devem permanecer em
`local.properties` ou nos secrets do GitHub Actions. Nunca coloques esses dados
no Git.

## Contribuir

Pull requests e issues são bem-vindos. Antes de contribuir:

1. Confirma que a alteração pertence ao Enhanced e não ao upstream original.
2. Mantém alterações focadas e explica o comportamento esperado.
3. Executa os testes e o lint relevantes.
4. Atualiza o changelog quando a alteração tiver impacto para utilizadores.
5. Não incluas tokens, passwords, credenciais ou ficheiros privados.

Para alterações grandes, abre primeiro uma issue para discutir a direção.

## Créditos E Atribuição

Este é um fork comunitário independente. **NuvioMobile Enhanced não é o projeto
original nem fala em nome dos maintainers do upstream.**

- Maintainer deste fork: [AKRusso](https://github.com/AKRusso)
- Projeto original: [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile)
- Organização upstream: [NuvioMedia](https://github.com/NuvioMedia)
- Asset de marca utilizado: [tapframe/NuvioTV](https://github.com/tapframe/NuvioTV)

As alterações específicas do fork estão no histórico Git, no changelog e nas
release notes. O código original permanece sujeito à sua licença e atribuições.

## Legal & DMCA

NuvioMobile Enhanced é uma interface client-side para navegar metadados e
reproduzir media através de extensões instaladas pelo utilizador e/ou fontes
fornecidas pelo utilizador. Usa-o apenas com conteúdo que possuas ou tenhas
autorização para aceder.

O projeto não aloja, guarda ou distribui conteúdo media e não é afiliado a
extensões, catálogos, fontes ou fornecedores de conteúdo terceiros.

- [Política legal e disclaimer](https://nuvioapp.space/legal)
- [Licença GPL-3.0](LICENSE)

## Histórico De Stars

<a href="https://www.star-history.com/#AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&theme=dark&legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left" />
  </picture>
</a>
