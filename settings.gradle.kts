rootProject.name = "theta"

include(
        "analysis",
        "cfa",
        "cfa-analysis",
        "cfa-cli",
        "common",
        "core",
        "mcm",
        "solver",
        "solver-z3",
        "sts",
        "sts-analysis",
        "sts-cli",
        "xcfa",
        "xcfa-cli-stateless",
        "xcfa-cli-por",
        "xcfa-analysis",
        "xcfa-cli-legacy",
        "xta",
        "xta-analysis",
        "xta-cli",
        "xsts",
        "xsts-analysis",
        "xsts-cli"
)

for (project in rootProject.children) {
    val projectName = project.name
    project.projectDir = file("subprojects/$projectName")
    project.name = "${rootProject.name}-$projectName"
}
