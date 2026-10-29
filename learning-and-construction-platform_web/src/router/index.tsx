import { useRoutes } from 'react-router-dom';
import routes from './router';

export default function AppRouter() {
  const element = useRoutes(routes);
  return element;
}
