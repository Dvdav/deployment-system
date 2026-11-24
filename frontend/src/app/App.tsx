import { Container, CssBaseline, Typography } from '@mui/material';

export default function App() {
  return (
    <>
      <CssBaseline />
      <Container component="main" maxWidth="md" sx={{ py: { xs: 4, sm: 8 } }}>
        <Typography component="h1" variant="h3">
          Deployment System
        </Typography>
      </Container>
    </>
  );
}
