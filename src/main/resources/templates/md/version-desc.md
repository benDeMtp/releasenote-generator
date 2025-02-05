{#each versions}
{#if !collapsed }### {/if}Version : {it.name} ({it.date})
{#if it.features.size > 0}
{#include md/section-desc.md title="Features 💰" commits=it.features /}
{/if}
{#if it.fixes.size > 0}
{#include md/section-desc.md title="Corrections 🩹" commits=it.fixes /}
{/if}
{#if it.refactors.size > 0}
{#include md/section-desc.md title="Refactorings 🚀" commits=it.refactors /}
{/if}
{#if it.tests.size > 0}
{#include md/section-desc.md title="Tests 🤖" commits=it.tests /}
{/if}
{#if it.builds.size > 0}
{#include md/section-desc.md title="Dépendances, versions, CI, etc. ⚙️" commits=it.builds /}
{/if}
{#if it.ops.size > 0}
{#include md/section-desc.md title="Infra, déploiement, etc. 📡" commits=it.ops /}
{/if}
{#if it.styles.size > 0}
{#include md/section-desc.md title="Style ✂️" commits=it.styles /}
{/if}
{#if it.docs.size > 0}
{#include md/section-desc.md title="Documentation 📜" commits=it.docs /}
{/if}
{#if it.chores.size > 0}{#include md/section-desc.md title="Divers" commits=it.chores /}{/if}{/each}