const root =
  "https://github.com/ivanezequiel545-creator/Sonic-keyboard/releases/tag/";
export const androidPreviews = [
  {
    stage: 1,
    tag: "v1.1.0-alpha.1",
    title: "La app",
    description:
      "Herramientas web empaquetadas para Android. No es un teclado del sistema.",
  },
  {
    stage: 2,
    tag: "v1.1.0-alpha.2",
    title: "Tu estilo nativo",
    description:
      "Prueba el teclado nativo, seis temas y una mano dentro de la app. Sin IME activable.",
  },
  {
    stage: 3,
    tag: "v1.1.0-alpha.3",
    title: "En tus otras apps",
    description:
      "Teclado Android real (IME). Actívalo y selecciónalo para escribir en campos de texto de otras apps.",
  },
].map((preview) => ({ ...preview, url: root + preview.tag }));
