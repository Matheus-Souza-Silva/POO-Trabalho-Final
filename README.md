# POO-Trabalho-Final

Notion de Acompanhamento do Trabalho: https://solux.notion.site/POO-Trabalho-em-Grupo-3f83d05adda5803f9eb5f7d682b56de3

## Banco de dados (MySQL 8)

Scripts em `sql/`, executados **nesta ordem** no MySQL Workbench:

| Arquivo | O que faz |
|---|---|
| `Champs_schema.sql` | cria o banco `campeonato`: tabelas, chaves primárias e estrangeiras, regras (CHECK) |
| `Champs_procedures_triggers.sql` | classificação automática e regras da partida |
| `Champs_relatorios.sql` | relatório principal (partidas por período) |

> O `Champs_schema.sql` **apaga e recria** o banco `campeonato`. Os dados do sistema são cadastrados pelas telas Java.

### Testes

- **Com Docker:** `bash sql/testar.sh` → cria um MySQL temporário, instala os scripts, roda `99_testes.sql` e termina com `TODOS OS TESTES PASSARAM`.
- **Sem Docker:** num banco recém-criado, rode `99_testes.sql` no Workbench: todo teste deve mostrar `OK`, e só os comandos marcados `ESPERADO ERRO` podem falhar.

Guia de estudo do banco: [`docs/guia/01-banco-de-dados.md`](docs/guia/01-banco-de-dados.md)
