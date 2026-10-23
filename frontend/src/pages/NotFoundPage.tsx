import { Link } from 'react-router'
import { EmptyState } from '@/components/ui/EmptyState'
import { Button } from '@/components/ui/Button'

export function NotFoundPage() {
  return <EmptyState title="Page not found" description="That page doesn't exist."
    action={<Link to="/"><Button>Go home</Button></Link>} />
}
